package com.futbol.majo.controller;

import com.futbol.majo.dto.MatchDto;
import com.futbol.majo.service.FootballDataService;
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
@RequestMapping("/api/football/laliga")
public class FootballController {

  private final FootballDataService footballDataService;

  public FootballController(FootballDataService footballDataService) {
    this.footballDataService = footballDataService;
  }

  /**
   * Sincroniza los partidos desde la API externa.
   */
  @PostMapping("/sync")
  public ResponseEntity<List<MatchDto>> syncMatches() {
    List<MatchDto> matches = footballDataService.syncAndSaveLaLigaMatches();
    return ResponseEntity.ok(matches);
  }

  /**
   * Obtiene partidos almacenados en BD con filtros opcionales por jornada, estado, equipo o rango de fechas.
   * Soporta tanto ?matchDay=1 como ?matchday=1 para mayor tolerancia en peticiones HTTP.
   */
  @GetMapping("/matches")
  public ResponseEntity<List<MatchDto>> getMatches(
      @RequestParam(name = "matchDay", required = false) Integer matchDay,
      @RequestParam(name = "matchday", required = false) Integer matchdayAlias,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "teamId", required = false) Long teamId,
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

    Integer effectiveMatchDay = matchDay != null ? matchDay : matchdayAlias;

    List<MatchDto> matches = footballDataService.getStoredMatchesFiltered(effectiveMatchDay, status, teamId, from, to);
    return ResponseEntity.ok(matches);
  }
}
