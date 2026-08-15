package com.futbol.majo.scheduler;

import com.futbol.majo.dto.League;
import com.futbol.majo.repository.MatchRepository;
import com.futbol.majo.service.FootballDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Planificador automático de sincronización de datos de fútbol.
 *
 * <p>Tiene dos responsabilidades:</p>
 * <ol>
 *   <li><b>Arranque:</b> cuando la app inicia, sincroniza automáticamente las ligas
 *       que no tienen ningún partido en la BD. Así el primer uso siempre tiene datos.</li>
 *   <li><b>Nocturnamente:</b> a las 3:00 AM sincroniza todas las ligas para mantener
 *       los resultados actualizados (partidos del día anterior, etc.).</li>
 * </ol>
 *
 * <p><b>Límite de la API gratuita:</b> 10 llamadas por minuto. Por eso esperamos
 * 7 segundos entre cada liga para no superar el límite.</p>
 */
@Slf4j
@Component
public class SyncScheduler {

  private final FootballDataService footballDataService;
  private final MatchRepository matchRepository;

  public SyncScheduler(FootballDataService footballDataService,
                       MatchRepository matchRepository) {
    this.footballDataService = footballDataService;
    this.matchRepository = matchRepository;
  }

  // =========================================================================
  // ARRANQUE: sincroniza solo las ligas sin datos
  // =========================================================================

  /**
   * Se ejecuta una vez cuando la aplicación ha arrancado completamente.
   *
   * <p>Para cada liga del enum {@link League}, comprueba si hay partidos en la BD.
   * Si no hay ninguno, lanza la sincronización. Si ya hay datos, los omite
   * para no desperdiciar llamadas a la API.</p>
   *
   * <p>Corre en un hilo demonio separado para no bloquear el servidor HTTP:
   * la app responde peticiones mientras sincroniza en segundo plano.</p>
   */
  @EventListener(ApplicationReadyEvent.class)
  public void syncMissingLeaguesOnStartup() {
    Thread syncThread = new Thread(() -> {
      log.info("=== Iniciando sincronización de ligas sin datos... ===");

      for (League league : League.values()) {
        try {
          long count = matchRepository.countByCompetitionCode(league.getCode());

          if (count == 0) {
            log.info("Liga {} sin datos — sincronizando...", league.getCode());
            footballDataService.syncAndSaveLaLigaMatches(league.getCode());
            log.info("Liga {} sincronizada correctamente.", league.getCode());

            // Esperamos 7 segundos para respetar el límite de 10 req/min
            Thread.sleep(7_000);
          } else {
            log.debug("Liga {} ya tiene {} partidos — omitida.",
                league.getCode(), count);
          }

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Sincronización de arranque interrumpida.");
          break;
        } catch (Exception e) {
          log.warn("No se pudo sincronizar {} en el arranque: {}",
              league.getCode(), e.getMessage());
        }
      }

      log.info("=== Sincronización de arranque completada. ===");
    }, "startup-sync-thread");

    // Hilo demonio: se detiene solo si la JVM cierra, no bloquea el shutdown
    syncThread.setDaemon(true);
    syncThread.start();
  }

  // =========================================================================
  // NOCTURNO: sincroniza todas las ligas cada noche
  // =========================================================================

  /**
   * Se ejecuta todos los días a las 3:00 AM para actualizar resultados.
   *
   * <p>Sincroniza todas las ligas secuencialmente con 7 segundos de pausa
   * entre cada una. Actualiza los marcadores de los partidos del día anterior.</p>
   *
   * <p>Expresión cron: {@code "0 0 3 * * *"} = segundo 0, minuto 0, hora 3,
   * cualquier día del mes, cualquier mes, cualquier día de la semana.</p>
   */
  @Scheduled(cron = "0 0 3 * * *")
  public void nightlySync() {
    log.info("=== Iniciando sincronización nocturna de todas las ligas... ===");

    for (League league : League.values()) {
      try {
        log.info("Sincronizando {} de forma nocturna...", league.getCode());
        footballDataService.syncAndSaveLaLigaMatches(league.getCode());
        Thread.sleep(7_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Sincronización nocturna interrumpida.");
        break;
      } catch (Exception e) {
        log.warn("Error en sincronización nocturna de {}: {}",
            league.getCode(), e.getMessage());
      }
    }

    log.info("=== Sincronización nocturna completada. ===");
  }
}
