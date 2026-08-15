package com.futbol.majo.dto;

import java.util.List;

public record MatchesResponseDTO(
    List<MatchDTO> matches
) {}