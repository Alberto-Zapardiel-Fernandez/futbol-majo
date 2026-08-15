package com.futbol.majo.repository;

import com.futbol.majo.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de Spring Data JPA para la gestión de persistencia de {@link MatchEntity}.
 *
 * <p>Spring genera automáticamente la implementación de {@code countByCompetitionCode}
 * a partir del nombre del método — no hace falta escribir ninguna query SQL.</p>
 */
public interface MatchRepository extends JpaRepository<MatchEntity, Long>,
    JpaSpecificationExecutor<MatchEntity> {

  /**
   * Cuenta los partidos almacenados para una competición concreta.
   * Se usa en el arranque para saber si una liga ya tiene datos o necesita sincronización.
   *
   * @param competitionCode Código de la competición (ej. "PD", "CL").
   * @return Número de partidos en BD para esa competición.
   */
  long countByCompetitionCode(String competitionCode);
}