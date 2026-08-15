package com.futbol.majo.repository;

import com.futbol.majo.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repositorio de Spring Data JPA para la gestión de persistencia de {@link MatchEntity}.
 *
 * <p>Spring genera automáticamente las implementaciones de todos los métodos
 * declarados aquí: los que siguen la convención de nombres (findBy..., countBy...)
 * y los que tienen una {@code @Query} JPQL explícita.</p>
 */
public interface MatchRepository extends JpaRepository<MatchEntity, Long>,
    JpaSpecificationExecutor<MatchEntity> {

  /**
   * Cuenta los partidos almacenados para una competición concreta.
   * Usado en el arranque para saber si una liga ya tiene datos.
   *
   * @param competitionCode Código de la competición (ej. "PD", "CL").
   * @return Número de partidos en BD para esa competición.
   */
  long countByCompetitionCode(String competitionCode);

  /**
   * Devuelve los códigos de competición que tienen partidos activos
   * (en juego, programados o a punto de empezar) en el rango de tiempo dado.
   *
   * <p>Lo usamos para el sync de partidos en vivo: en lugar de sincronizar
   * todas las ligas cada 5 minutos, solo sincronizamos las que tienen
   * partidos en la ventana horaria actual. Más eficiente y respetuoso
   * con el límite de 10 llamadas/minuto de la API gratuita.</p>
   *
   * @param start  Inicio de la ventana temporal (normalmente, ahora - 3h).
   * @param end    Fin de la ventana temporal (normalmente, ahora + 4h).
   * @return Lista de códigos únicos de competición con partidos activos.
   */
  @Query("""
        SELECT DISTINCT m.competitionCode
        FROM MatchEntity m
        WHERE m.status IN ('TIMED', 'IN_PLAY', 'SCHEDULED', 'PAUSED')
          AND m.utcDate BETWEEN :start AND :end
        """)
  List<String> findActiveCompetitionCodesForPeriod(
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end
  );
}
