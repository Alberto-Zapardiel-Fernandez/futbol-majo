package com.futbol.majo.mapper;

import com.futbol.majo.dto.MatchDTO;
import com.futbol.majo.dto.TeamDTO;
import com.futbol.majo.entity.MatchEntity;
import com.futbol.majo.entity.TeamEntity;
import org.springframework.stereotype.Component;

/**
 * Componente encargado de la transformación entre objetos DTO y entidades JPA de dominio.
 */
@Component
public class MatchMapper {

  /**
   * Convierte un {@link TeamDTO} en una entidad {@link TeamEntity}.
   *
   * @param dto DTO del equipo recibido de la API o capa externa.
   * @return Instancia mapeada de {@link TeamEntity}.
   */
  public TeamEntity toTeamEntity(TeamDTO dto) {
    if (dto == null) {
      return null;
    }
    return TeamEntity.builder()
        .id(dto.id())
        .name(dto.name())
        .shortName(dto.shortName())
        .crest(dto.crest())
        .build();
  }

  /**
   * Convierte una entidad {@link TeamEntity} a su DTO correspondiente {@link TeamDTO}.
   *
   * @param entity Entidad de persistencia del equipo.
   * @return Instancia mapeada de {@link TeamDTO}.
   */
  public TeamDTO toTeamDto(TeamEntity entity) {
    if (entity == null) {
      return null;
    }
    return new TeamDTO(
        entity.getId(),
        entity.getName(),
        entity.getShortName(),
        entity.getCrest()
    );
  }

  /**
   * Convierte un {@link MatchDTO} en una entidad {@link MatchEntity}.
   *
   * @param dto DTO del partido.
   * @return Instancia mapeada de {@link MatchEntity}.
   */
  public MatchEntity toMatchEntity(MatchDTO dto) {
    if (dto == null) {
      return null;
    }
    return MatchEntity.builder()
        .id(dto.id())
        .status(dto.status())
        .utcDate(dto.utcDate())
        .matchDay(dto.matchDay())
        .homeTeam(toTeamEntity(dto.homeTeam()))
        .awayTeam(toTeamEntity(dto.awayTeam()))
        .build();
  }

  /**
   * Convierte una entidad {@link MatchEntity} a su DTO correspondiente {@link MatchDTO}.
   *
   * @param entity Entidad de persistencia del partido.
   * @return Instancia mapeada de {@link MatchDTO}.
   */
  public MatchDTO toMatchDto(MatchEntity entity) {
    if (entity == null) {
      return null;
    }
    return new MatchDTO(
        entity.getId(),
        entity.getStatus(),
        entity.getUtcDate(),
        entity.getMatchDay(),
        toTeamDto(entity.getHomeTeam()),
        toTeamDto(entity.getAwayTeam())
    );
  }
}
