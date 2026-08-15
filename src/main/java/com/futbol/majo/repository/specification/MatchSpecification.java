package com.futbol.majo.repository.specification;

import com.futbol.majo.entity.MatchEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Especificaciones JPA dinámicas para la filtración de partidos.
 */
public class MatchSpecification {

  private MatchSpecification() {
    // Clase de utilidad
  }

  /**
   * Filtra partidos por número de jornada.
   */
  public static Specification<MatchEntity> hasMatchday(Integer matchday) {
    return (root, query, cb) ->
        matchday == null ? null : cb.equal(root.get("matchDay"), matchday);
  }

  /**
   * Filtra por competición.
   *
   * @param competition La competición (código) a filtrar.
   * @return La especificación JPA correspondiente.
   */
  public static Specification<MatchEntity> hasCompetition(String competition) {
    return (root, query, cb) ->
        (competition == null || competition.isBlank()) ? null : cb.equal(cb.upper(root.get("competitionCode")), competition.trim().toUpperCase());
  }

  /**
   * Filtra partidos por estado (FINISHED, SCHEDULED, IN_PLAY, etc.).
   */
  public static Specification<MatchEntity> hasStatus(String status) {
    return (root, query, cb) ->
        (status == null || status.isBlank()) ? null : cb.equal(root.get("status"), status.toUpperCase());
  }

  /**
   * Filtra partidos donde el equipo indicado sea local O visitante.
   */
  public static Specification<MatchEntity> hasTeamId(Long teamId) {
    return (root, query, cb) -> {
      if (teamId == null) {
        return null;
      }
      return cb.or(
          cb.equal(root.get("homeTeam").get("id"), teamId),
          cb.equal(root.get("awayTeam").get("id"), teamId)
      );
    };
  }

  /**
   * Filtra partidos cuya fecha esté dentro de un rango determinado.
   */
  public static Specification<MatchEntity> betweenDates(OffsetDateTime from, OffsetDateTime to) {
    return (root, query, cb) -> {
      if (from == null && to == null) {
        return null;
      }
      if (from != null && to != null) {
        return cb.between(root.get("utcDate"), from, to);
      }
      if (from != null) {
        return cb.greaterThanOrEqualTo(root.get("utcDate"), from);
      }
      return cb.lessThanOrEqualTo(root.get("utcDate"), to);
    };
  }
}
