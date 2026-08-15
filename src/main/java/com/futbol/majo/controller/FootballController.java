package com.futbol.majo.controller;

import com.futbol.majo.dto.MatchDto;
import com.futbol.majo.service.FootballDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión y consulta de los datos futbolísticos de LaLiga.
 */
@RestController
@RequestMapping("/api/football")
public class FootballController {

  private final FootballDataService footballDataService;

  /**
   * Constructor para la inyección del servicio de datos de fútbol.
   *
   * @param footballDataService Servicio de negocio de datos deportivos.
   */
  public FootballController(FootballDataService footballDataService) {
    this.footballDataService = footballDataService;
  }

  /**
   * Sincroniza los partidos desde la API externa hacia la base de datos local.
   *
   * @return {@link ResponseEntity} con la lista de partidos sincronizados.
   */
  @PostMapping("/laliga/sync")
  public ResponseEntity<List<MatchDto>> syncMatches() {
    List<MatchDto> syncedMatches = footballDataService.syncAndSaveLaLigaMatches();
    return ResponseEntity.ok(syncedMatches);
  }

  /**
   * Obtiene la lista de partidos guardados previamente en la base de datos.
   *
   * @return {@link ResponseEntity} con la lista de partidos de la base de datos.
   */
  @GetMapping("/laliga/matches")
  public ResponseEntity<List<MatchDto>> getStoredMatches() {
    List<MatchDto> matches = footballDataService.getStoredMatches();
    return ResponseEntity.ok(matches);
  }
}
