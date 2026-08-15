package com.futbol.majo.repository;

import com.futbol.majo.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data JPA para la gestión de persistencia de la entidad {@link MatchEntity}.
 */
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {
}
