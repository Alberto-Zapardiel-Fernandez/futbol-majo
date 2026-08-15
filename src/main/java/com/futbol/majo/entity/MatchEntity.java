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
 * Entidad JPA que representa un partido de fútbol almacenado en Supabase.
 *
 * <p>Contiene los datos básicos del partido: equipos, fecha, estado, jornada
 * y marcador final. Los goles ({@code homeScore}, {@code awayScore}) se
 * rellenan al sincronizar y pueden ser null para partidos no disputados.</p>
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEntity {

  /** Identificador único del partido asignado por la API externa. */
  @Id
  private Long id;

  /** Código de la competición (ej. "PD" para LaLiga, "CL" para Champions). */
  @Column(name = "competition_code")
  private String competitionCode;

  /** Estado actual: SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED, POSTPONED... */
  @Column(name = "status", nullable = false)
  private String status;

  /** Fecha y hora del partido en UTC. */
  @Column(name = "utc_date")
  private OffsetDateTime utcDate;

  /** Número de jornada dentro de la competición. */
  @Column(name = "match_day")
  private Integer matchDay;

  /** Equipo local. */
  @ManyToOne
  @JoinColumn(name = "home_team_id")
  private TeamEntity homeTeam;

  /** Equipo visitante. */
  @ManyToOne
  @JoinColumn(name = "away_team_id")
  private TeamEntity awayTeam;

  /**
   * Goles del equipo local al final del partido.
   * Null si el partido no ha comenzado o no hay datos de goles.
   */
  @Column(name = "home_score")
  private Integer homeScore;

  /**
   * Goles del equipo visitante al final del partido.
   * Null si el partido no ha comenzado o no hay datos de goles.
   */
  @Column(name = "away_score")
  private Integer awayScore;
}
