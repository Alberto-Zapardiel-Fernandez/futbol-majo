package com.futbol.majo.dto;

public record TableEntryDTO(
    Integer position,
    TeamDTO team,
    Integer playedGames,
    String form,
    Integer won,
    Integer draw,
    Integer lost,
    Integer points,
    Integer goalsFor,
    Integer goalsAgainst,
    Integer goalDifference
) {}