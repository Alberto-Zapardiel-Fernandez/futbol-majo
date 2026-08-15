package com.futbol.majo.dto;

import java.time.LocalDate;

public record MatchCriteriaDTO(
    Integer matchDay,
    Long teamId,
    String status,
    LocalDate startDate,
    LocalDate endDate
) {}