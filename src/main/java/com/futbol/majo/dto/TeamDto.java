package com.futbol.majo.dto;

public record TeamDto(
    Long id,
    String name,
    String shortName,
    String crest
) {}