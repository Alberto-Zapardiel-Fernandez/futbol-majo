package com.futbol.majo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeración de todas las competiciones soportadas por la API de football-data.org
 * y por esta aplicación.
 *
 * <p>Cada constante encapsula el código oficial de la competición y su nombre
 * legible. Este enum es la fuente de verdad para el frontend: el selector de
 * ligas del menú principal debe construirse a partir de estos valores.</p>
 *
 * <p>El código ({@link #getCode()}) es el que se pasa como parámetro en las
 * llamadas a los endpoints {@code /sync}, {@code /matches} y {@code /standings}.</p>
 */
@Getter
@AllArgsConstructor
public enum League {

  /** Copa del Mundo FIFA. */
  WC("WC", "FIFA World Cup"),

  /** Liga de Campeones de la UEFA. */
  CL("CL", "UEFA Champions League"),

  /** Bundesliga (Alemania). */
  BL1("BL1", "Bundesliga"),

  /** Eredivisie (Países Bajos). */
  DED("DED", "Eredivisie"),

  /** Campeonato Brasileiro Série A (Brasil). */
  BSA("BSA", "Campeonato Brasileiro Série A"),

  /** Primera División — LaLiga (España). */
  PD("PD", "Primera División"),

  /** Ligue 1 (Francia). */
  FL1("FL1", "Ligue 1"),

  /** Championship (Segunda división inglesa). */
  ELC("ELC", "Championship"),

  /** Primeira Liga (Portugal). */
  PPL("PPL", "Primeira Liga"),

  /** Campeonato Europeo de Naciones. */
  EC("EC", "European Championship"),

  /** Serie A (Italia). */
  SA("SA", "Serie A"),

  /** Premier League (Inglaterra). */
  PL("PL", "Premier League");

  // -------------------------------------------------------------------------

  private final String code;
  private final String name;

}
