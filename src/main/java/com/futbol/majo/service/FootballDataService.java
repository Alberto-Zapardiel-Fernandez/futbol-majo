package com.futbol.majo.service;

import com.futbol.majo.dto.MatchDto;
import com.futbol.majo.dto.MatchesResponseDto;
import com.futbol.majo.dto.TeamDto;
import com.futbol.majo.mapper.MatchMapper;
import com.futbol.majo.repository.MatchRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Servicio encargado de la orquestación de datos de partidos de fútbol,
 * consumo de la API externa e integración masiva mediante JDBC Batch en la base de datos.
 */
@Service
public class FootballDataService {

  private final RestClient footballRestClient;
  private final MatchRepository matchRepository;
  private final MatchMapper matchMapper;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor para la inyección de dependencias.
   *
   * @param footballRestClient Cliente HTTP configurado para la API externa.
   * @param matchRepository Repositorio JPA para la consulta de partidos.
   * @param matchMapper Componente mapeador de entidades y DTOs.
   * @param jdbcTemplate Cliente JDBC de Spring para ejecución masiva en lote.
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

  /**
   * Sincroniza los partidos desde la API externa guardándolos en la base de datos
   * mediante sentencias PostgreSQL UPSERT en lote (Batch JDBC) para ejecutar la operación
   * en un único viaje de red.
   *
   * @return Lista de DTOs de los partidos recibidos y guardados.
   */
  @Transactional
  public List<MatchDto> syncAndSaveLaLigaMatches() {
    MatchesResponseDto response = footballRestClient.get()
        .uri("/competitions/PD/matches")
        .retrieve()
        .body(MatchesResponseDto.class);

    if (response == null || response.matches() == null || response.matches().isEmpty()) {
      return List.of();
    }

    List<MatchDto> matches = response.matches();

    // 1. Extraer y deduplicar equipos recibidos
    List<TeamDto> uniqueTeams = matches.stream()
        .flatMap(m -> Stream.of(m.homeTeam(), m.awayTeam()))
        .filter(t -> t != null && t.id() != null)
        .collect(Collectors.toMap(
            TeamDto::id,
            Function.identity(),
            (existing, replacement) -> existing
        ))
        .values()
        .stream()
        .toList();

    // 2. Insertar o actualizar equipos en lote (UPSERT masivo)
    String teamUpsertSql = """
                INSERT INTO teams (id, name, short_name, crest)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    short_name = EXCLUDED.short_name,
                    crest = EXCLUDED.crest
                """;

    jdbcTemplate.batchUpdate(teamUpsertSql, uniqueTeams, uniqueTeams.size(),
        (PreparedStatement ps, TeamDto team) -> {
          ps.setLong(1, team.id());
          ps.setString(2, team.name());
          ps.setString(3, team.shortName());
          ps.setString(4, team.crest());
        });

    // 3. Insertar o actualizar partidos en lote (UPSERT masivo)
    String matchUpsertSql = """
                INSERT INTO matches (id, status, utc_date, home_team_id, away_team_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    utc_date = EXCLUDED.utc_date,
                    home_team_id = EXCLUDED.home_team_id,
                    away_team_id = EXCLUDED.away_team_id
                """;

    jdbcTemplate.batchUpdate(matchUpsertSql, matches, matches.size(),
        (PreparedStatement ps, MatchDto match) -> {
          ps.setLong(1, match.id());
          ps.setString(2, match.status());
          ps.setObject(3, match.utcDate());
          ps.setObject(4, match.homeTeam() != null ? match.homeTeam().id() : null);
          ps.setObject(5, match.awayTeam() != null ? match.awayTeam().id() : null);
        });

    return matches;
  }

  /**
   * Recupera todos los partidos almacenados en la base de datos.
   *
   * @return Lista de DTOs de los partidos persistidos.
   */
  @Transactional(readOnly = true)
  public List<MatchDto> getStoredMatches() {
    return matchRepository.findAll().stream()
        .map(matchMapper::toMatchDto)
        .toList();
  }
}