package com.futbol.majo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de arranque de la aplicación "Fútbol Majo".
 *
 * <p>{@code @EnableScheduling} activa el motor de tareas programadas de Spring.
 * Sin esta anotación, los {@code @Scheduled} del {@link com.futbol.majo.scheduler.SyncScheduler}
 * no tienen efecto. Es el interruptor general de las tareas automáticas.</p>
 */
@SpringBootApplication
@EnableScheduling
public class MajoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MajoApplication.class, args);
	}
}