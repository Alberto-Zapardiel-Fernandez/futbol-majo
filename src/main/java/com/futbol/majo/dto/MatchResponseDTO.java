package com.futbol.majo.dto;

import java.time.LocalDateTime;

public record MatchResponseDTO(
    Long id,
    Integer matchDay,
    Long homeTeamId,
    Long awayTeamId,
    String homeTeamName,
    String awayTeamName,
    Integer homeScore,
    Integer awayScore,
    LocalDateTime matchDate,
    String status
) {}