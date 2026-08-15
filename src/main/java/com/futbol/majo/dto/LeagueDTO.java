package com.futbol.majo.dto;

/**
 * DTO de respuesta que representa una competición soportada por la aplicación.
 *
 * <p>Se utiliza en el endpoint {@code GET /api/football/leagues} para devolver
 * la lista de ligas disponibles al frontend. El frontend usará esta lista para
 * construir el selector de competiciones en la UI.</p>
 *
 * @param code Código oficial de la competición (ej. "PD", "CL", "PL").
 *             Es el valor que el frontend debe enviar en las demás llamadas API.
 * @param name Nombre legible de la competición (ej. "Primera División").
 *             Es el texto que se muestra al usuario en la interfaz.
 */
public record LeagueDTO(
    String code,
    String name
) {}