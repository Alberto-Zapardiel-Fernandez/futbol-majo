package com.futbol.majo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;

public record MatchDTO(
    Long id,
    String status,
    OffsetDateTime utcDate,
    @JsonAlias("matchday") Integer matchDay,
    TeamDTO homeTeam,
    TeamDTO awayTeam
) {}