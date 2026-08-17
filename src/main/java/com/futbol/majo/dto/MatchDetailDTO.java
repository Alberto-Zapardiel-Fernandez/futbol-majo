package com.futbol.majo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO con el detalle completo de un partido, incluyendo alineaciones.
 *
 * <p>Las alineaciones ({@link LineupDTO}) solo están disponibles cuando
 * el partido ha comenzado. Antes del inicio, {@code lineups} llega vacío.</p>
 */
public record MatchDetailDTO(
    Long              id,
    String            status,
    OffsetDateTime    utcDate,
    @JsonAlias("matchday") Integer matchDay,
    TeamDTO           homeTeam,
    TeamDTO           awayTeam,
    ScoreDTO          score,
    List<LineupDTO>   lineups
) {

  /**
   * Alineación de un equipo: formación y jugadores titulares y suplentes.
   */
  public record LineupDTO(
      TeamDTO         team,
      String          formation,
      List<PlayerDTO> startXI,
      List<PlayerDTO> substitutes
  ) {}

  /** Wrapper del jugador tal como lo devuelve la API. */
  public record PlayerDTO(
      PlayerInfoDTO player
  ) {}

  /** Datos básicos de un jugador. */
  public record PlayerInfoDTO(
      Long    id,
      String  name,
      String  position,
      Integer shirtNumber
  ) {}
}
