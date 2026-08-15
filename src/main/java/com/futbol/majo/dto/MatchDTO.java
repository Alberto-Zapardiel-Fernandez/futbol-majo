package com.futbol.majo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;

/**
 * DTO que representa un partido de fútbol.
 *
 * <p>Se usa tanto para deserializar la respuesta de football-data.org
 * como para devolver datos al frontend. Jackson mapea automáticamente
 * el campo {@code matchday} (minúsculas de la API) a {@code matchDay}
 * gracias a {@code @JsonAlias}.</p>
 *
 * @param id       Identificador único del partido.
 * @param status   Estado: SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED...
 * @param utcDate  Fecha y hora del partido en UTC.
 * @param matchDay Número de jornada.
 * @param homeTeam Equipo local.
 * @param awayTeam Equipo visitante.
 * @param score    Marcador del partido. Null si aún no ha empezado.
 */
public record MatchDTO(
    Long id,
    String status,
    OffsetDateTime utcDate,
    @JsonAlias("matchday") Integer matchDay,
    TeamDTO homeTeam,
    TeamDTO awayTeam,
    ScoreDTO score
) {}