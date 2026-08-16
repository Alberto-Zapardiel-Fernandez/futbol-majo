package com.futbol.majo.repository;

import com.futbol.majo.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repositorio JPA para la gestión de partidos.
 *
 * <p>Spring genera las implementaciones automáticamente:
 * - Métodos con convención de nombres (countBy..., findBy...)
 * - Métodos con @Query JPQL explícita</p>
 */
public interface MatchRepository extends JpaRepository<MatchEntity, Long>,
    JpaSpecificationExecutor<MatchEntity> {

  /**
   * Cuenta los partidos de una competición concreta.
   * Usado en el arranque para saber si una liga ya tiene datos.
   *
   * @param competitionCode Código de la competición (ej. "PD").
   * @return Número de partidos en BD para esa competición.
   */
  long countByCompetitionCode(String competitionCode);

  /**
   * Devuelve códigos de competición que necesitan sincronización inmediata.
   *
   * <p>Incluye dos casos:</p>
   * <ol>
   *   <li>Partidos con estado IN_PLAY o PAUSED en BD, independientemente de la fecha.
   *       Estos son datos <b>obsoletos</b>: el partido terminó pero el backend no
   *       lo actualizó (apagado, error de red, etc.). Hay que sincronizarlos
   *       sin importar cuándo fue el partido.</li>
   *   <li>Partidos TIMED o SCHEDULED cuya fecha está dentro de la ventana
   *       {@code [start, end]} (normalmente las próximas 2 horas). Se sincronizan
   *       para actualizar el status antes del inicio.</li>
   * </ol>
   *
   * <p>Esta query es mucho más robusta que filtrar solo por fecha, porque
   * captura partidos de días anteriores que quedaron con estado incorrecto.</p>
   *
   * @param start Inicio de la ventana para partidos futuros (ahora).
   * @param end   Fin de la ventana para partidos futuros (ahora + 2h).
   * @return Lista de códigos únicos de competición que necesitan sync.
   */
  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('IN_PLAY', 'PAUSED')
           OR (m.status IN ('TIMED', 'SCHEDULED') AND m.utcDate BETWEEN :start AND :end)
        """)
  List<String> findCompetitionsNeedingSync(
      @Param("start") OffsetDateTime start,
      @Param("end")   OffsetDateTime end
  );

  /**
   * Devuelve los códigos de competición que tienen partidos con estado
   * IN_PLAY o PAUSED en la BD (datos obsoletos que no se actualizaron).
   *
   * <p>Se usa en el arranque del servidor para detectar y corregir
   * resultados que quedaron sin actualizar de sesiones anteriores.</p>
   *
   * @return Lista de códigos de competición con partidos atascados en IN_PLAY/PAUSED.
   */
  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('IN_PLAY', 'PAUSED')
        """)
  List<String> findCompetitionsWithStaleLiveMatches();
}