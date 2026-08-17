package com.futbol.majo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;

/**
 * DTO que representa un partido de fútbol.
 *
 * <p>Añadido {@code competitionCode} para que el frontend pueda agrupar
 * los partidos por liga en la sección "En Vivo y Próximos".</p>
 */
public record MatchDTO(
    Long            id,
    String          competitionCode,
    String          status,
    OffsetDateTime  utcDate,
    @JsonAlias("matchday") Integer matchDay,
    TeamDTO         homeTeam,
    TeamDTO         awayTeam,
    ScoreDTO        score
) {}