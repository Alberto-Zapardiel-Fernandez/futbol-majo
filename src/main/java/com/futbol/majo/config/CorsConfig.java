package com.futbol.majo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración CORS.
 * Los orígenes permitidos se configuran via variable de entorno ALLOWED_ORIGINS.
 * En local: <a href="http://localhost:3000">...</a>
 * En producción: añadimos la URL de Vercel en las variables de Render.
 */
@Configuration
public class CorsConfig {

  @Value("${allowed.origins:http://localhost:3000}")
  private String allowedOrigins;

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();

    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .toList();

    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST"));
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return new CorsFilter(source);
  }
}
