package com.futbol.majo.mapper;

import com.futbol.majo.dto.MatchDto;
import com.futbol.majo.dto.TeamDto;
import com.futbol.majo.entity.MatchEntity;
import com.futbol.majo.entity.TeamEntity;
import org.springframework.stereotype.Component;

/**
 * Componente encargado de la transformación entre objetos DTO y entidades JPA de dominio.
 */
@Component
public class MatchMapper {

  /**
   * Convierte un {@link TeamDto} en una entidad {@link TeamEntity}.
   *
   * @param dto DTO del equipo recibido de la API o capa externa.
   * @return Instancia mapeada de {@link TeamEntity}.
   */
  public TeamEntity toTeamEntity(TeamDto dto) {
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
   * Convierte una entidad {@link TeamEntity} a su DTO correspondiente {@link TeamDto}.
   *
   * @param entity Entidad de persistencia del equipo.
   * @return Instancia mapeada de {@link TeamDto}.
   */
  public TeamDto toTeamDto(TeamEntity entity) {
    if (entity == null) {
      return null;
    }
    return new TeamDto(
        entity.getId(),
        entity.getName(),
        entity.getShortName(),
        entity.getCrest()
    );
  }

  /**
   * Convierte un {@link MatchDto} en una entidad {@link MatchEntity}.
   *
   * @param dto DTO del partido.
   * @return Instancia mapeada de {@link MatchEntity}.
   */
  public MatchEntity toMatchEntity(MatchDto dto) {
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
   * Convierte una entidad {@link MatchEntity} a su DTO correspondiente {@link MatchDto}.
   *
   * @param entity Entidad de persistencia del partido.
   * @return Instancia mapeada de {@link MatchDto}.
   */
  public MatchDto toMatchDto(MatchEntity entity) {
    if (entity == null) {
      return null;
    }
    return new MatchDto(
        entity.getId(),
        entity.getStatus(),
        entity.getUtcDate(),
        entity.getMatchDay(),
        toTeamDto(entity.getHomeTeam()),
        toTeamDto(entity.getAwayTeam())
    );
  }
}
