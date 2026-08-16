package com.futbol.majo.repository;

import com.futbol.majo.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repositorio JPA para partidos.
 */
public interface MatchRepository extends JpaRepository<MatchEntity, Long>,
    JpaSpecificationExecutor<MatchEntity> {

  long countByCompetitionCode(String competitionCode);

  /**
   * Competiciones que necesitan sync ahora mismo.
   * Captura tres casos:
   * 1. Partidos ya marcados IN_PLAY o PAUSED en BD (actualizar marcador).
   * 2. Partidos TIMED/SCHEDULED cuya hora ya pasó pero aún no se han actualizado
   *    (el partido de las 19:00 a las 19:15 — este era el bug).
   * 3. Partidos TIMED/SCHEDULED a punto de empezar (próximas 2h).
   */
  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('IN_PLAY', 'PAUSED')
           OR (m.status IN ('TIMED', 'SCHEDULED') AND m.utcDate BETWEEN :windowStart AND :windowEnd)
        """)
  List<String> findCompetitionsNeedingSync(
      @Param("windowStart") OffsetDateTime windowStart,
      @Param("windowEnd")   OffsetDateTime windowEnd
  );

  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('IN_PLAY', 'PAUSED')
        """)
  List<String> findCompetitionsWithStaleLiveMatches();
}