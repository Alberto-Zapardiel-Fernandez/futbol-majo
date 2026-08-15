package com.futbol.majo.service;

import com.futbol.majo.dto.MatchDTO;
import com.futbol.majo.dto.MatchesResponseDTO;
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
 *   <li>Sincronizar partidos desde la API externa a nuestra BD (Supabase).</li>
 *   <li>Consultar partidos de nuestra BD con filtros dinámicos.</li>
 *   <li>Obtener la clasificación en tiempo real desde la API externa, con caché.</li>
 * </ul>
 *
 * <p>La caché (Caffeine) se configura en {@code application.yaml} y se activa
 * con {@code @EnableCaching} en {@link com.futbol.majo.MajoApplication}.</p>
 */
@Service
public class FootballDataService {

  private final RestClient footballRestClient;
  private final MatchRepository matchRepository;
  private final MatchMapper matchMapper;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor con inyección de dependencias explícita (sin @Autowired, mejor práctica).
   *
   * @param footballRestClient Cliente HTTP configurado para la API de football-data.org.
   * @param matchRepository    Repositorio JPA para la entidad {@link com.futbol.majo.entity.MatchEntity}.
   * @param matchMapper        Conversor entre entidades JPA y DTOs.
   * @param jdbcTemplate       Template JDBC para operaciones batch de alto rendimiento.
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
  // SINCRONIZACIÓN (escribe en BD + invalida la caché de clasificación)
  // =========================================================================

  /**
   * Sincroniza todos los partidos de una liga desde la API externa hacia nuestra BD.
   *
   * <p>Usa un Batch UPSERT con JDBC para máximo rendimiento:
   * si el partido ya existe, lo actualiza; si no, lo inserta.
   * Esta operación también invalida la caché de clasificación ({@code @CacheEvict})
   * porque tras sincronizar, los standings pueden haber cambiado.</p>
   *
   * @param league Código de la competición (ej. "PD" para LaLiga, "BL1" para Bundesliga).
   *               Si es {@code null}, se usa "PD" por defecto.
   * @return Lista de {@link MatchDTO} sincronizados desde la API.
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

    // 1. Deduplicar e insertar equipos en lote
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
                name = EXCLUDED.name,
                short_name = EXCLUDED.short_name,
                crest = EXCLUDED.crest
            """;

    jdbcTemplate.batchUpdate(teamUpsertSql, uniqueTeams, uniqueTeams.size(),
        (PreparedStatement ps, TeamDTO team) -> {
          ps.setLong(1, team.id());
          ps.setString(2, team.name());
          ps.setString(3, team.shortName());
          ps.setString(4, team.crest());
        });

    // 2. Insertar/actualizar partidos en lote incluyendo competition_code
    String matchUpsertSql = """
            INSERT INTO matches (id, competition_code, status, utc_date, match_day, home_team_id, away_team_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                competition_code = EXCLUDED.competition_code,
                status           = EXCLUDED.status,
                utc_date         = EXCLUDED.utc_date,
                match_day        = EXCLUDED.match_day,
                home_team_id     = EXCLUDED.home_team_id,
                away_team_id     = EXCLUDED.away_team_id
            """;

    jdbcTemplate.batchUpdate(matchUpsertSql, matches, matches.size(),
        (PreparedStatement ps, MatchDTO match) -> {
          ps.setLong(1, match.id());
          ps.setString(2, competitionCode);
          ps.setString(3, match.status());
          ps.setObject(4, match.utcDate());
          ps.setObject(5, match.matchDay());
          ps.setObject(6, match.homeTeam() != null ? match.homeTeam().id() : null);
          ps.setObject(7, match.awayTeam() != null ? match.awayTeam().id() : null);
        });

    return matches;
  }

  // =========================================================================
  // CONSULTA DE PARTIDOS (desde nuestra BD, sin caché — ya es rápido con JPA)
  // =========================================================================

  /**
   * Consulta dinámica de partidos con filtros opcionales y paginación.
   *
   * <p>Utiliza JPA Specifications para construir la query SQL de forma segura
   * y programática. Al consultar nuestra propia BD (Supabase) no necesitamos
   * caché aquí; la caché es para proteger la API externa.</p>
   *
   * @param competition Código de la competición (ej. "PD", "BL1"). Opcional.
   * @param matchDay    Número de jornada. Opcional.
   * @param status      Estado del partido (FINISHED, SCHEDULED, IN_PLAY...). Opcional.
   * @param teamId      ID del equipo (busca como local O visitante). Opcional.
   * @param from        Fecha de inicio del rango. Opcional.
   * @param to          Fecha de fin del rango. Opcional.
   * @param pageable    Configuración de paginación y ordenación de Spring Data.
   * @return Página de {@link MatchDTO} que cumplen los filtros.
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
  // CLASIFICACIÓN (desde la API externa, CON caché de 60 minutos)
  // =========================================================================

  /**
   * Obtiene la clasificación actual de una competición desde la API externa.
   *
   * <p><b>Caché activa:</b> el resultado se almacena en la caché {@code "standings"}
   * durante 60 minutos (configurado en {@code application.yaml}).
   * La clave de caché es el código de competición normalizado a mayúsculas,
   * así "pd", "PD" y "Pd" comparten la misma entrada en caché.</p>
   *
   * <p>Esto protege el límite de la API gratuita: aunque 1000 usuarios
   * abran la clasificación a la vez, solo se lanza 1 llamada real a la API
   * cada 60 minutos.</p>
   *
   * @param competitionCode Código de la competición (ej. "PD" para LaLiga).
   *                        Si es {@code null} o vacío, se usa "PD" por defecto.
   * @return {@link StandingsResponseDTO} con la tabla de clasificación completa.
   */
  @Cacheable(
      value = "standings",
      key   = "#competitionCode != null ? #competitionCode.trim().toUpperCase() : 'PD'"
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
}