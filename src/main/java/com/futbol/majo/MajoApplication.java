package com.futbol.majo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de arranque de la aplicación "Fútbol Majo".
 *
 * <p>La configuración de caché se ha trasladado a {@link com.futbol.majo.config.CacheConfig}
 * para que el {@code CacheManager} esté disponible tanto en producción como en los
 * tests de capa ({@code @DataJpaTest}), que no cargan las autoConfiguraciones de caché.</p>
 */
@SpringBootApplication
public class MajoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MajoApplication.class, args);
	}
}