package com.futbol.majo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para la aplicación.
 *
 * <p><b>¿Qué es CORS?</b><br>
 * Cuando el frontend (Next.js en puerto 3000) llama al backend (Spring Boot en
 * puerto 8080), el navegador comprueba si el backend permite peticiones desde
 * ese origen. Sin esta configuración, el navegador bloquea todas las llamadas
 * con un error "CORS policy" aunque el backend funcione perfectamente.</p>
 *
 * <p>En desarrollo permitimos {@code localhost:3000}. En producción añadiremos
 * la URL de Vercel cuando hagamos el despliegue (Día 9).</p>
 */
@Configuration
public class CorsConfig {

  /**
   * Registra el filtro de CORS con las reglas permitidas por entorno.
   *
   * <p>Configuración actual (desarrollo):</p>
   * <ul>
   *   <li>Origen permitido: {@code http://localhost:3000} (Next.js en local).</li>
   *   <li>Métodos permitidos: GET, POST (los que usa nuestra API).</li>
   *   <li>Headers permitidos: todos (para no bloquear el header JSON).</li>
   *   <li>Credenciales: desactivadas (no usamos cookies todavía).</li>
   * </ul>
   *
   * @return El filtro CORS configurado que Spring registra automáticamente.
   */
  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();

    // Orígenes que pueden llamar a la API
    // En producción añadiremos aquí la URL de Vercel
    config.setAllowedOrigins(List.of(
        "http://localhost:3000"
    ));

    // Métodos HTTP que permitimos
    config.setAllowedMethods(List.of("GET", "POST"));

    // Headers que puede enviar el frontend
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);

    return new CorsFilter(source);
  }
}
