package com.futbol.majo.dto;

import java.util.List;

public record MatchesResponseDto(
    List<MatchDto> matches
) {}