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
   * Competiciones que necesitan sync ahora.
   * Incluye LIVE además de IN_PLAY y PAUSED — football-data.org
   * devuelve "LIVE" en el tier gratuito en lugar de "IN_PLAY".
   */
  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('IN_PLAY', 'PAUSED', 'LIVE')
           OR (m.status IN ('TIMED', 'SCHEDULED') AND m.utcDate BETWEEN :windowStart AND :windowEnd)
        """)
  List<String> findCompetitionsNeedingSync(
      @Param("windowStart") OffsetDateTime windowStart,
      @Param("windowEnd")   OffsetDateTime windowEnd
  );

  /**
   * Partidos atascados en estado live sin actualizar.
   * Incluye LIVE además de IN_PLAY y PAUSED.
   */
  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('IN_PLAY', 'PAUSED', 'LIVE')
        """)
  List<String> findCompetitionsWithStaleLiveMatches();
}