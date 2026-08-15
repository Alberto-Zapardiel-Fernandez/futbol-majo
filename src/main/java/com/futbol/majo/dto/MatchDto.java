package com.futbol.majo.dto;

import java.time.OffsetDateTime;

public record MatchDto(
    Long id,
    String status,
    OffsetDateTime utcDate,
    TeamDto homeTeam,
    TeamDto awayTeam
) {}