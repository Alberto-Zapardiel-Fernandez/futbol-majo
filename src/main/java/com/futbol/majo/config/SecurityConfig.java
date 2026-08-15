package com.futbol.majo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad HTTP de la aplicación.
 *
 * <p>Por ahora la política es abierta: todos los endpoints de la API
 * ({@code /api/**}) y los de la documentación Swagger son públicos.
 * Cuando implementemos el login de Google en la fase de frontend,
 * añadiremos aquí las reglas de autenticación OAuth2.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Lista blanca de rutas de Swagger UI que deben ser accesibles sin autenticación.
   * Incluye la página HTML, los recursos estáticos de la UI y el JSON de la spec.
   */
  private static final String[] SWAGGER_WHITELIST = {
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/v3/api-docs/**",
      "/v3/api-docs.yaml"
  };

  /**
   * Configura la cadena de filtros de seguridad HTTP.
   *
   * <p>Reglas actuales:</p>
   * <ul>
   *   <li>CSRF desactivado (la API es stateless, no usa sesiones de navegador).</li>
   *   <li>{@code /api/**} — acceso libre para todos los endpoints de negocio.</li>
   *   <li>Rutas de Swagger — acceso libre para poder consultar la documentación.</li>
   *   <li>Resto de rutas — requieren autenticación (preparado para OAuth2 futuro).</li>
   * </ul>
   *
   * @param http Constructor de la cadena de seguridad proporcionado por Spring.
   * @return La cadena de filtros de seguridad configurada.
   * @throws Exception Si ocurre algún error durante la configuración.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/**").permitAll()
            .requestMatchers(SWAGGER_WHITELIST).permitAll()
            .anyRequest().authenticated()
        )
        .build();
  }
}
