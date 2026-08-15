package com.futbol.majo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidad JPA que representa un partido de fútbol y su relación con los equipos participantes.
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEntity {

  /**
   * Identificador único del partido asignado por la API externa.
   */
  @Id
  private Long id;

  /**
   * A que liga pertenece
   */
  @Column(name = "competition_code")
  private String competitionCode;

  /**
   * Estado del partido (ej. SCHEDULED, TIMED, FINISHED).
   */
  @Column(name = "status", nullable = false)
  private String status;

  /**
   * Fecha y hora programada para el encuentro en formato UTC.
   */
  @Column(name = "utc_date")
  private OffsetDateTime utcDate;

  /**
   * Día del partido
   */
  @Column(name = "match_day")
  private Integer matchDay;

  /**
   * Equipo que disputa el partido en condición de local.
   */
  @ManyToOne
  @JoinColumn(name = "home_team_id")
  private TeamEntity homeTeam;

  /**
   * Equipo que disputa el partido en condición de visitante.
   */
  @ManyToOne
  @JoinColumn(name = "away_team_id")
  private TeamEntity awayTeam;
}
