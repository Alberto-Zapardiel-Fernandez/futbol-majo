package com.futbol.majo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa un equipo de fútbol registrado en el sistema.
 */
@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamEntity {

  /**
   * Identificador único del equipo asignado por la API externa.
   */
  @Id
  private Long id;

  /**
   * Nombre oficial completo del equipo.
   */
  private String name;

  /**
   * Nombre abreviado o comercial del equipo.
   */
  private String shortName;

  /**
   * URL del escudo oficial del equipo.
   */
  private String crest;

}
