package com.futbol.majo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;

public record MatchDto(
    Long id,
    String status,
    OffsetDateTime utcDate,
    @JsonAlias("matchday") Integer matchDay,
    TeamDto homeTeam,
    TeamDto awayTeam
) {}