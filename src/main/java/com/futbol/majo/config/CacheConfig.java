package com.futbol.majo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración explícita del sistema de caché de la aplicación.
 *
 * <p><b>¿Por qué una clase separada?</b><br>
 * Al tener {@code @EnableCaching} en {@code MajoApplication}, los tests de capa
 * ({@code @DataJpaTest}) fallaban porque no incluyen la autoconfiguración de caché
 * de Spring Boot. Al moverlo aquí como una clase {@code @Configuration} explícita,
 * los tests SÍ la cargan y el {@code CacheManager} siempre está disponible.</p>
 *
 * <p><b>Motor elegido — Caffeine:</b><br>
 * Caffeine es la librería de caché en memoria más rápida para JVM.
 * Usamos un único caché llamado {@code "standings"} con:</p>
 * <ul>
 *   <li>{@code maximumSize=200} — máximo 200 entradas distintas en memoria.</li>
 *   <li>{@code expireAfterWrite=60min} — cada entrada caduca 60 minutos después
 *       de escribirse, protegiéndo el límite de la API gratuita de football-data.org.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Crea y registra el {@link CacheManager} principal de la aplicación.
   *
   * <p>Usa Caffeine como motor de caché en memoria. La configuración es
   * intencional y explícita para que funcione en todos los contextos
   * (producción, tests de integración y tests de capa JPA).</p>
   *
   * @return Un {@link CaffeineCacheManager} configurado con TTL de 60 minutos
   *         y un máximo de 200 entradas.
   */
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(60, TimeUnit.MINUTES)
    );
    return manager;
  }
}
