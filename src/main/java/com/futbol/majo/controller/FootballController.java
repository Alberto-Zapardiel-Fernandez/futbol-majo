package com.futbol.majo.controller;

import com.futbol.majo.dto.LeagueDTO;
import com.futbol.majo.dto.League;
import com.futbol.majo.dto.MatchDTO;
import com.futbol.majo.dto.StandingsResponseDTO;
import com.futbol.majo.service.FootballDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Controlador REST principal para la gestión de partidos y datos de fútbol.
 *
 * <p>Expone cuatro operaciones fundamentales:</p>
 * <ol>
 *   <li>Listado de competiciones soportadas.</li>
 *   <li>Sincronización de partidos desde la API externa hacia Supabase.</li>
 *   <li>Consulta de partidos almacenados con filtros dinámicos y paginación.</li>
 *   <li>Consulta de la clasificación actual de una competición (con caché).</li>
 * </ol>
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/football/laliga")
@Tag(
    name = "Football",
    description = "Endpoints para ligas, partidos, clasificaciones y sincronización de datos"
)
public class FootballController {

  private final FootballDataService footballDataService;

  // =========================================================================
  // LIGAS SOPORTADAS
  // =========================================================================

  /**
   * Devuelve la lista estática de todas las competiciones soportadas.
   *
   * <p>El frontend usa este endpoint para construir el selector de ligas.
   * La lista se genera a partir del enum {@link League}, que es la fuente
   * de verdad de las competiciones disponibles.</p>
   *
   * @return Lista de {@link LeagueDTO} con el código y nombre de cada competición.
   */
  @GetMapping("/leagues")
  @Operation(
      summary = "Ligas soportadas",
      description = "Devuelve la lista completa de competiciones disponibles en la app. " +
          "El frontend usa esta lista para el selector de ligas. " +
          "El campo 'code' es el valor que hay que enviar en el parámetro " +
          "'competition' del resto de endpoints."
  )
  public ResponseEntity<List<LeagueDTO>> getSupportedLeagues() {
    List<LeagueDTO> leagues = Arrays.stream(League.values())
        .map(league -> new LeagueDTO(league.getCode(), league.getName()))
        .toList();
    return ResponseEntity.ok(leagues);
  }

  // =========================================================================
  // SINCRONIZACIÓN
  // =========================================================================

  /**
   * Sincroniza los partidos de una competición desde la API externa hacia Supabase.
   *
   * @param league Código de la liga a sincronizar (ej. "PD", "BL1"). Por defecto "PD".
   * @return Lista de partidos sincronizados como {@link MatchDTO}.
   */
  @PostMapping("/sync")
  @Operation(
      summary = "Sincronizar partidos",
      description = "Descarga todos los partidos de la temporada de la liga indicada " +
          "desde football-data.org y los guarda/actualiza en Supabase. " +
          "También invalida la caché de clasificación. " +
          "Usa GET /leagues para ver los códigos válidos."
  )
  public ResponseEntity<List<MatchDTO>> syncMatches(
      @Parameter(description = "Código de la competición. Por defecto LaLiga (PD)")
      @RequestParam(required = false) String league) {

    List<MatchDTO> matches = footballDataService.syncAndSaveLaLigaMatches(league);
    return ResponseEntity.ok(matches);
  }

  // =========================================================================
  // PARTIDOS
  // =========================================================================

  /**
   * Obtiene partidos almacenados en BD con filtros opcionales y paginación.
   *
   * @param competition Código de la competición (ej. "PD").
   * @param matchDay    Número de jornada.
   * @param status      Estado: SCHEDULED, TIMED, IN_PLAY, FINISHED, POSTPONED.
   * @param teamId      ID del equipo (busca como local O visitante).
   * @param from        Fecha de inicio del rango (formato ISO-8601).
   * @param to          Fecha de fin del rango (formato ISO-8601).
   * @param pageable    Parámetros de paginación: page, size, sort.
   * @return Página de {@link MatchDTO} que cumplen los filtros.
   */
  @GetMapping("/matches")
  @Operation(
      summary = "Consultar partidos",
      description = "Devuelve partidos almacenados en BD con filtros opcionales y combinables. " +
          "Paginación con: ?page=0&size=20&sort=utcDate,asc"
  )
  public ResponseEntity<Page<MatchDTO>> getMatches(
      @Parameter(description = "Código de competición (PD, CL, PL...)")
      @RequestParam(required = false) String competition,

      @Parameter(description = "Número de jornada (1, 2, 3...)")
      @RequestParam(required = false) Integer matchDay,

      @Parameter(description = "Estado: SCHEDULED, TIMED, IN_PLAY, FINISHED, POSTPONED")
      @RequestParam(required = false) String status,

      @Parameter(description = "ID del equipo (busca como local Y como visitante)")
      @RequestParam(required = false) Long teamId,

      @Parameter(description = "Fecha inicio (ISO-8601 ej: 2026-08-01T00:00:00Z)")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,

      @Parameter(description = "Fecha fin (ISO-8601 ej: 2026-08-31T23:59:59Z)")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,

      @PageableDefault(size = 20, sort = "utcDate") Pageable pageable) {

    Page<MatchDTO> matches = footballDataService.getStoredMatchesFiltered(
        competition, matchDay, status, teamId, from, to, pageable
    );
    return ResponseEntity.ok(matches);
  }

  // =========================================================================
  // CLASIFICACIÓN
  // =========================================================================

  /**
   * Devuelve la clasificación actual de una competición.
   *
   * @param competition Código de la competición (por defecto "PD").
   * @return {@link StandingsResponseDTO} con la tabla de clasificación.
   */
  @GetMapping("/standings")
  @Operation(
      summary = "Clasificación",
      description = "Devuelve la clasificación desde football-data.org con caché de 60 minutos. " +
          "Se invalida automáticamente al ejecutar /sync."
  )
  public ResponseEntity<StandingsResponseDTO> getStandings(
      @Parameter(description = "Código de la competición. Por defecto LaLiga (PD)")
      @RequestParam(required = false, defaultValue = "PD") String competition) {

    return ResponseEntity.ok(footballDataService.getStandings(competition));
  }
}
