package com.futbol.majo.repository;

import com.futbol.majo.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data JPA para la gestión de persistencia de la entidad {@link TeamEntity}.
 */
public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
}
