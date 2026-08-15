package com.futbol.majo.dto;

import java.util.List;

public record StandingsResponseDTO(
    List<StandingGroupDTO> standings
) {}