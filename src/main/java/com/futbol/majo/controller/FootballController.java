package com.futbol.majo.controller;

import com.futbol.majo.dto.MatchDTO;
import com.futbol.majo.dto.StandingsResponseDTO;
import com.futbol.majo.service.FootballDataService;
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
import java.util.List;

/**
 * Controlador REST para la gestión de partidos y datos de fútbol.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/football/laliga")
public class FootballController {

  private final FootballDataService footballDataService;

  /**
   * Sincroniza los partidos desde la API externa.
   */
  @PostMapping("/sync")
  public ResponseEntity<List<MatchDTO>> syncMatches(@RequestParam(required = false) String league) {
    List<MatchDTO> matches = footballDataService.syncAndSaveLaLigaMatches(league);
    return ResponseEntity.ok(matches);
  }

  /**
   * Obtiene partidos almacenados en BD con filtros opcionales por jornada, estado, equipo o rango de fechas.
   * Soporta tanto ?matchDay=1 como ?matchday=1 para mayor tolerancia en peticiones HTTP.
   */
  @GetMapping("/matches")
  public ResponseEntity<Page<MatchDTO>> getMatches(
      @RequestParam(required = false) String competition,
      @RequestParam(required = false) Integer matchDay,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long teamId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @PageableDefault(size = 20, sort = "utcDate") Pageable pageable) {

    Page<MatchDTO> matches = footballDataService.getStoredMatchesFiltered(
        competition, matchDay, status, teamId, from, to, pageable
    );

    return ResponseEntity.ok(matches);
  }

  /**
   * Devuelve la clasificación actual de una competición.
   * Ejemplo: GET /api/football/laliga/standings?competition=PD
   */
  @GetMapping("/standings")
  public ResponseEntity<StandingsResponseDTO> getStandings(
      @RequestParam(required = false, defaultValue = "PD") String competition) {

    return ResponseEntity.ok(footballDataService.getStandings(competition.trim().toUpperCase()));
  }

}
