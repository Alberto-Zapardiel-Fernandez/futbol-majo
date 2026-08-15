package com.futbol.majo.mapper;

import com.futbol.majo.dto.MatchDTO;
import com.futbol.majo.dto.ScoreDTO;
import com.futbol.majo.dto.TeamDTO;
import com.futbol.majo.entity.MatchEntity;
import com.futbol.majo.entity.TeamEntity;
import org.springframework.stereotype.Component;

/**
 * Conversor entre entidades JPA y DTOs de dominio.
 *
 * <p>Separa la capa de persistencia de la capa de presentación:
 * el controller nunca ve entidades JPA, solo DTOs.</p>
 */
@Component
public class MatchMapper {

  /**
   * Convierte un {@link TeamDTO} en entidad {@link TeamEntity}.
   */
  public TeamEntity toTeamEntity(TeamDTO dto) {
    if (dto == null) return null;
    return TeamEntity.builder()
        .id(dto.id())
        .name(dto.name())
        .shortName(dto.shortName())
        .crest(dto.crest())
        .build();
  }

  /**
   * Convierte una entidad {@link TeamEntity} en su DTO {@link TeamDTO}.
   */
  public TeamDTO toTeamDto(TeamEntity entity) {
    if (entity == null) return null;
    return new TeamDTO(entity.getId(), entity.getName(),
        entity.getShortName(), entity.getCrest());
  }

  /**
   * Convierte un {@link MatchDTO} en entidad {@link MatchEntity}.
   */
  public MatchEntity toMatchEntity(MatchDTO dto) {
    if (dto == null) return null;

    ScoreDTO.FullTimeDTO ft = dto.score() != null ? dto.score().fullTime() : null;

    return MatchEntity.builder()
        .id(dto.id())
        .status(dto.status())
        .utcDate(dto.utcDate())
        .matchDay(dto.matchDay())
        .homeTeam(toTeamEntity(dto.homeTeam()))
        .awayTeam(toTeamEntity(dto.awayTeam()))
        .homeScore(ft != null ? ft.home() : null)
        .awayScore(ft != null ? ft.away() : null)
        .build();
  }

  /**
   * Convierte una entidad {@link MatchEntity} en su DTO {@link MatchDTO}.
   *
   * <p>Reconstruye el objeto {@link ScoreDTO} desde las columnas planas
   * {@code homeScore} y {@code awayScore} de la entidad.</p>
   */
  public MatchDTO toMatchDto(MatchEntity entity) {
    if (entity == null) return null;

    // Reconstruimos el ScoreDTO solo si hay datos de goles
    ScoreDTO score = null;
    if (entity.getHomeScore() != null || entity.getAwayScore() != null) {
      score = new ScoreDTO(
          new ScoreDTO.FullTimeDTO(entity.getHomeScore(), entity.getAwayScore()),
          null
      );
    }

    return new MatchDTO(
        entity.getId(),
        entity.getStatus(),
        entity.getUtcDate(),
        entity.getMatchDay(),
        toTeamDto(entity.getHomeTeam()),
        toTeamDto(entity.getAwayTeam()),
        score
    );
  }
}
