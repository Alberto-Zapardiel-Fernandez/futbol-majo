package com.futbol.majo.dto;

import java.util.List;

/**
 * DTO con el detalle completo de un equipo incluyendo plantilla.
 *
 * <p>Obtenido de {@code /teams/{id}} de football-data.org.
 * El campo {@code squad} contiene todos los jugadores de la plantilla
 * con posición, dorsal y nacionalidad.</p>
 */
public record TeamDetailDTO(
    Long              id,
    String            name,
    String            shortName,
    String            crest,
    String            venue,        // Nombre del estadio
    Integer           founded,      // Año de fundación
    String            clubColors,   // Ej: "Red / Blue"
    String            website,
    List<SquadMemberDTO> squad
) {

  /**
   * Jugador de la plantilla.
   *
   * @param id          ID del jugador.
   * @param name        Nombre completo.
   * @param position    Posición en inglés (ej. "Centre-Back", "Goalkeeper").
   * @param dateOfBirth Fecha de nacimiento en formato "yyyy-MM-dd".
   * @param nationality Nacionalidad del jugador.
   * @param shirtNumber Dorsal. Puede ser null si aún no asignado.
   */
  public record SquadMemberDTO(
      Long    id,
      String  name,
      String  position,
      String  dateOfBirth,
      String  nationality,
      Integer shirtNumber
  ) {}
}
