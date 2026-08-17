package com.futbol.majo.service;

import com.futbol.majo.dto.MatchDTO;
import com.futbol.majo.dto.MatchDetailDTO;
import com.futbol.majo.dto.MatchesResponseDTO;
import com.futbol.majo.dto.ScoreDTO;
import com.futbol.majo.dto.StandingsResponseDTO;
import com.futbol.majo.dto.TeamDTO;
import com.futbol.majo.entity.MatchEntity;
import com.futbol.majo.mapper.MatchMapper;
import com.futbol.majo.repository.MatchRepository;
import com.futbol.majo.repository.specification.MatchSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Servicio principal que orquesta todos los datos de fútbol.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Sincronizar partidos (incluidos los marcadores) desde la API externa.</li>
 *   <li>Consultar partidos de nuestra BD con filtros dinámicos.</li>
 *   <li>Obtener clasificaciones desde la API externa (con caché de 60 min).</li>
 * </ul>
 */
@Service
public class FootballDataService {

  private final RestClient footballRestClient;
  private final MatchRepository matchRepository;
  private final MatchMapper matchMapper;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor con inyección de dependencias explícita.
   */
  public FootballDataService(RestClient footballRestClient,
                             MatchRepository matchRepository,
                             MatchMapper matchMapper,
                             JdbcTemplate jdbcTemplate) {
    this.footballRestClient = footballRestClient;
    this.matchRepository = matchRepository;
    this.matchMapper = matchMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  // =========================================================================
  // SINCRONIZACIÓN
  // =========================================================================

  /**
   * Sincroniza partidos y marcadores desde la API externa hacia Supabase.
   *
   * <p>Usa Batch UPSERT con JDBC: si el partido ya existe lo actualiza
   * (útil para actualizar el marcador tras el partido), si no existe lo inserta.
   * También invalida la caché de clasificación al sincronizar.</p>
   *
   * @param league Código de la liga (ej. "PD"). Si es null usa "PD".
   * @return Lista de partidos sincronizados.
   */
  @CacheEvict(value = "standings", allEntries = true)
  @Transactional
  public List<MatchDTO> syncAndSaveLaLigaMatches(String league) {
    String competitionCode = league != null ? league.trim().toUpperCase() : "PD";

    MatchesResponseDTO response = footballRestClient.get()
        .uri("/competitions/" + competitionCode + "/matches")
        .retrieve()
        .body(MatchesResponseDTO.class);

    if (response == null || response.matches() == null || response.matches().isEmpty()) {
      return List.of();
    }

    List<MatchDTO> matches = response.matches();

    // 1. Equipos — deduplicar e insertar en lote
    List<TeamDTO> uniqueTeams = matches.stream()
        .flatMap(m -> Stream.of(m.homeTeam(), m.awayTeam()))
        .filter(t -> t != null && t.id() != null)
        .collect(Collectors.toMap(
            TeamDTO::id,
            Function.identity(),
            (existing, replacement) -> existing
        ))
        .values()
        .stream()
        .toList();

    String teamUpsertSql = """
        INSERT INTO teams (id, name, short_name, crest)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            name       = EXCLUDED.name,
            short_name = EXCLUDED.short_name,
            crest      = EXCLUDED.crest
        """;

    jdbcTemplate.batchUpdate(teamUpsertSql, uniqueTeams, uniqueTeams.size(),
        (PreparedStatement ps, TeamDTO team) -> {
          ps.setLong(1, team.id());
          ps.setString(2, team.name());
          ps.setString(3, team.shortName());
          ps.setString(4, team.crest());
        });

    // 2. Partidos — insertar/actualizar incluyendo marcadores
    String matchUpsertSql = """
        INSERT INTO matches
            (id, competition_code, status, utc_date, match_day,
             home_team_id, away_team_id, home_score, away_score)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            competition_code = EXCLUDED.competition_code,
            status           = EXCLUDED.status,
            utc_date         = EXCLUDED.utc_date,
            match_day        = EXCLUDED.match_day,
            home_team_id     = EXCLUDED.home_team_id,
            away_team_id     = EXCLUDED.away_team_id,
            home_score       = EXCLUDED.home_score,
            away_score       = EXCLUDED.away_score
        """;

    jdbcTemplate.batchUpdate(matchUpsertSql, matches, matches.size(),
        (PreparedStatement ps, MatchDTO match) -> {
          // Extraemos los goles del objeto score anidado (puede ser null)
          ScoreDTO.FullTimeDTO ft = match.score() != null
              ? match.score().fullTime()
              : null;

          ps.setLong(1, match.id());
          ps.setString(2, competitionCode);
          ps.setString(3, match.status());
          ps.setObject(4, match.utcDate());
          ps.setObject(5, match.matchDay());
          ps.setObject(6, match.homeTeam() != null ? match.homeTeam().id() : null);
          ps.setObject(7, match.awayTeam() != null ? match.awayTeam().id() : null);
          ps.setObject(8, ft != null ? ft.home() : null);
          ps.setObject(9, ft != null ? ft.away() : null);
        });

    return matches;
  }

  // =========================================================================
  // CONSULTA DE PARTIDOS
  // =========================================================================

  /**
   * Consulta dinámica de partidos con filtros opcionales y paginación.
   *
   * @param competition Código de la competición. Opcional.
   * @param matchDay    Número de jornada. Opcional.
   * @param status      Estado del partido. Opcional.
   * @param teamId      ID del equipo. Opcional.
   * @param from        Fecha inicio del rango. Opcional.
   * @param to          Fecha fin del rango. Opcional.
   * @param pageable    Paginación y ordenación de Spring Data.
   * @return Página de {@link MatchDTO} con los filtros aplicados.
   */
  @Transactional(readOnly = true)
  public Page<MatchDTO> getStoredMatchesFiltered(String competition,
                                                 Integer matchDay,
                                                 String status,
                                                 Long teamId,
                                                 OffsetDateTime from,
                                                 OffsetDateTime to,
                                                 Pageable pageable) {
    Specification<MatchEntity> spec = Specification
        .where(MatchSpecification.hasCompetition(competition))
        .and(MatchSpecification.hasMatchday(matchDay))
        .and(MatchSpecification.hasStatus(status))
        .and(MatchSpecification.hasTeamId(teamId))
        .and(MatchSpecification.betweenDates(from, to));

    return matchRepository.findAll(spec, pageable)
        .map(matchMapper::toMatchDto);
  }

  // =========================================================================
  // CLASIFICACIÓN (con caché)
  // =========================================================================

  /**
   * Obtiene la clasificación actual desde la API externa con caché de 60 minutos.
   *
   * @param competitionCode Código de la competición. Si es null usa "PD".
   * @return Clasificación completa de la competición.
   */
  @Cacheable(
      value = "standings",
      key = "#competitionCode != null ? #competitionCode.trim().toUpperCase() : 'PD'"
  )
  public StandingsResponseDTO getStandings(String competitionCode) {
    String code = (competitionCode != null && !competitionCode.isBlank())
        ? competitionCode.trim().toUpperCase()
        : "PD";

    return footballRestClient.get()
        .uri("/competitions/" + code + "/standings")
        .retrieve()
        .body(StandingsResponseDTO.class);
  }

  /**
   * Devuelve partidos en curso y próximos de todas las ligas.
   *
   * <p>Estrategia en dos pasos para que la sección nunca quede vacía:</p>
   * <ol>
   *   <li>Busca partidos live (IN_PLAY, LIVE, PAUSED) + próximas 12 horas.</li>
   *   <li>Si no hay ninguno, devuelve los 10 siguientes partidos programados
   *       sin límite de tiempo — útil en días sin partidos cercanos.</li>
   * </ol>
   *
   * @return Lista de {@link MatchDTO} ordenada por {@code utcDate} ascendente.
   */
  @Transactional(readOnly = true)
  public List<MatchDTO> getLiveAndUpcomingMatches() {
    OffsetDateTime now   = OffsetDateTime.now();
    OffsetDateTime in12h = now.plusHours(12);

    // Paso 1 — partidos live o en las próximas 12 horas
    Specification<MatchEntity> live = (root, q, cb) ->
        root.get("status").in("IN_PLAY", "LIVE", "PAUSED");

    Specification<MatchEntity> soon = (root, q, cb) -> cb.and(
        root.get("status").in("TIMED", "SCHEDULED"),
        cb.between(root.get("utcDate"), now, in12h)
    );

    List<MatchDTO> results = matchRepository
        .findAll(
            Specification.where(live).or(soon),
            org.springframework.data.domain.Sort.by("utcDate").ascending()
        )
        .stream()
        .map(matchMapper::toMatchDto)
        .toList();

    if (!results.isEmpty()) return results;

    // Paso 2 — fallback: próximos 10 partidos sin límite de tiempo
    Specification<MatchEntity> nextMatches = (root, q, cb) -> cb.and(
        root.get("status").in("TIMED", "SCHEDULED"),
        cb.greaterThan(root.get("utcDate"), now)
    );

    return matchRepository
        .findAll(
            nextMatches,
            org.springframework.data.domain.PageRequest.of(
                0, 10,
                org.springframework.data.domain.Sort.by("utcDate").ascending()
            )
        )
        .getContent()
        .stream()
        .map(matchMapper::toMatchDto)
        .toList();
  }

  /**
   * Obtiene el detalle completo de un partido desde la API externa.
   *
   * <p>Incluye score actualizado y alineaciones cuando están disponibles.
   * Las alineaciones solo aparecen a partir del inicio del partido.</p>
   *
   * <p>No se cachea porque los datos cambian durante el partido.</p>
   *
   * @param matchId ID del partido en football-data.org.
   * @return {@link MatchDetailDTO} con toda la información del partido.
   */
  public MatchDetailDTO getMatchDetail(Long matchId) {
    return footballRestClient.get()
        .uri("/matches/" + matchId)
        .retrieve()
        .body(MatchDetailDTO.class);
  }

}
