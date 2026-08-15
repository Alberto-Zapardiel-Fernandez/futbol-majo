package com.futbol.majo.dto;

import java.util.List;

public record StandingGroupDTO(
    String stage,
    String type,
    List<TableEntryDTO> table
) {}