package com.futbol.majo.config;

import com.futbol.majo.dto.League;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Configuración de la documentación OpenAPI 3 (Swagger) de la aplicación.
 *
 * <p>Define los metadatos de la API: título, descripción y versión.
 * La lista de ligas se genera automáticamente desde el enum {@link League},
 * así siempre está sincronizada con las competiciones realmente soportadas.</p>
 *
 * <p>URLs disponibles al arrancar la app:</p>
 * <ul>
 *   <li><b>Swagger UI:</b> <a href="http://localhost:8080/swagger-ui.html">...</a></li>
 *   <li><b>OpenAPI JSON:</b> http://localhost:8080/v3/api-docs</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

  /**
   * Crea el bean {@link OpenAPI} con los metadatos del proyecto.
   *
   * <p>La tabla de ligas soportadas se construye dinámicamente iterando
   * el enum {@link League}, de modo que al añadir una nueva liga al enum
   * también aparece automáticamente en la documentación Swagger.</p>
   *
   * @return Instancia de {@link OpenAPI} configurada con info del proyecto.
   */
  @Bean
  public OpenAPI footballMajoOpenAPI() {

    // Generamos la tabla de ligas dinámicamente desde el enum
    String leaguesTable = Arrays.stream(League.values())
        .map(l -> String.format("| `%s` | %s |", l.getCode(), l.getName()))
        .collect(Collectors.joining("\n"));

    String description = """
            API REST para consultar partidos de fútbol, clasificaciones y sincronización
            de datos desde [football-data.org](https://www.football-data.org).
            
            ---
            
            **Ligas soportadas** — usa el código en el parámetro `competition`:
            
            | Código | Competición |
            |--------|-------------|
            """ + leaguesTable + """
            
            ---
            
            **Límite de la API gratuita:** 10 llamadas/minuto.
            La clasificación (`/standings`) se cachea 60 minutos para proteger este límite.
            """;

    return new OpenAPI()
        .info(new Info()
            .title("Fútbol Majo API")
            .description(description)
            .version("1.0.0")
            .contact(new Contact()
                .name("Fútbol Majo")
                .email("admin@futbolmajo.com")));
  }
}
