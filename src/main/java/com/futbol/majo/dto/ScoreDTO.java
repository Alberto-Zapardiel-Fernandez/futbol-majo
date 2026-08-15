package com.futbol.majo.dto;

/**
 * DTO que representa el marcador de un partido.
 *
 * <p>Replica la estructura que devuelve football-data.org en el campo "score".
 * Los records anidados {@link FullTimeDTO} y {@link HalfTimeDTO} mapean
 * los goles del tiempo reglamentario y del primer tiempo respectivamente.</p>
 *
 * <p>Los valores pueden ser {@code null} en partidos no disputados todavía.</p>
 */
public record ScoreDTO(
    FullTimeDTO fullTime,
    HalfTimeDTO halfTime
) {

  /**
   * Goles al final del partido (o en el momento actual si está en juego).
   *
   * @param home Goles del equipo local.
   * @param away Goles del equipo visitante.
   */
  public record FullTimeDTO(Integer home, Integer away) {}

  /**
   * Goles en el descanso.
   *
   * @param home Goles del equipo local en el primer tiempo.
   * @param away Goles del equipo visitante en el primer tiempo.
   */
  public record HalfTimeDTO(Integer home, Integer away) {}
}
