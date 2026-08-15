package com.futbol.majo.scheduler;

import com.futbol.majo.dto.League;
import com.futbol.majo.repository.MatchRepository;
import com.futbol.majo.service.FootballDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Planificador automático de sincronización de datos de fútbol.
 *
 * <p>Tres responsabilidades:</p>
 * <ol>
 *   <li><b>Arranque:</b> sincroniza las ligas sin datos en BD para que el primer
 *       uso siempre tenga algo que mostrar.</li>
 *   <li><b>Cada 5 minutos:</b> sincroniza solo las ligas con partidos en curso o
 *       a punto de empezar. Así los estados IN_PLAY, goles y resultados se
 *       actualizan en tiempo casi real sin sobrepasar el límite de la API.</li>
 *   <li><b>Cada noche a las 3am:</b> sincroniza todas las ligas para mantener
 *       los resultados del día completamente actualizados.</li>
 * </ol>
 *
 * <p><b>Límite de la API gratuita:</b> 10 llamadas/minuto. Esperamos 7 segundos
 * entre ligas para no superarlo nunca.</p>
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
  // ARRANQUE: sincroniza las ligas sin datos
  // =========================================================================

  /**
   * Se ejecuta una vez cuando la aplicación ha arrancado completamente.
   *
   * <p>Para cada liga del enum {@link League}, comprueba si hay partidos en BD.
   * Si no hay ninguno, lanza la sincronización. Si ya hay datos, la omite
   * para no gastar llamadas de la API.</p>
   *
   * <p>Corre en un hilo separado para no bloquear el servidor HTTP.</p>
   */
  @EventListener(ApplicationReadyEvent.class)
  public void syncMissingLeaguesOnStartup() {
    Thread syncThread = new Thread(() -> {
      log.info("=== Sincronización de arranque: buscando ligas sin datos... ===");

      for (League league : League.values()) {
        try {
          long count = matchRepository.countByCompetitionCode(league.getCode());

          if (count == 0) {
            log.info("[Arranque] {} sin datos — sincronizando...", league.getCode());
            footballDataService.syncAndSaveLaLigaMatches(league.getCode());
            log.info("[Arranque] {} completada.", league.getCode());
            Thread.sleep(7_000);
          } else {
            log.debug("[Arranque] {} ya tiene {} partidos — omitida.",
                league.getCode(), count);
          }

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("[Arranque] Sincronización interrumpida.");
          break;
        } catch (Exception e) {
          log.warn("[Arranque] Error en {}: {}", league.getCode(), e.getMessage());
        }
      }

      log.info("=== Sincronización de arranque completada. ===");
    }, "startup-sync-thread");

    syncThread.setDaemon(true);
    syncThread.start();
  }

  // =========================================================================
  // CADA 5 MINUTOS: solo las ligas con partidos activos ahora
  // =========================================================================

  /**
   * Se ejecuta cada 5 minutos y sincroniza únicamente las ligas que tienen
   * partidos en juego o a punto de empezar en la ventana horaria actual.
   *
   * <p>La ventana de búsqueda en BD es: ahora - 3 horas hasta ahora + 4 horas.
   * Esto captura partidos que empezaron hace rato (IN_PLAY) y los que empiezan
   * pronto (TIMED/SCHEDULED).</p>
   *
   * <p>{@code fixedDelay = 300_000} = 300.000ms = 5 minutos. A diferencia de
   * {@code fixedRate}, el temporizador empieza a contar desde que termina
   * la ejecución anterior, no desde que empieza. Más seguro ante ejecuciones lentas.</p>
   */
  @Scheduled(fixedDelay = 300_000)
  public void syncLiveAndUpcomingMatches() {
    OffsetDateTime start = OffsetDateTime.now().minusHours(3);
    OffsetDateTime end   = OffsetDateTime.now().plusHours(4);

    List<String> activeCodes = matchRepository
        .findActiveCompetitionCodesForPeriod(start, end);

    if (activeCodes.isEmpty()) {
      log.debug("[Live sync] No hay partidos activos en esta ventana horaria.");
      return;
    }

    log.info("[Live sync] Sincronizando {} liga(s) con partidos activos: {}",
        activeCodes.size(), activeCodes);

    for (String code : activeCodes) {
      try {
        footballDataService.syncAndSaveLaLigaMatches(code);
        log.info("[Live sync] {} actualizada.", code);
        Thread.sleep(7_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[Live sync] Sincronización interrumpida.");
        break;
      } catch (Exception e) {
        log.warn("[Live sync] Error en {}: {}", code, e.getMessage());
      }
    }
  }

  // =========================================================================
  // NOCTURNO: todas las ligas a las 3am
  // =========================================================================

  /**
   * Sincroniza todas las ligas cada noche a las 3:00 AM.
   * Actualiza los resultados finales de los partidos del día anterior.
   */
  @Scheduled(cron = "0 0 3 * * *")
  public void nightlySync() {
    log.info("=== Sincronización nocturna iniciada... ===");

    for (League league : League.values()) {
      try {
        footballDataService.syncAndSaveLaLigaMatches(league.getCode());
        log.info("[Nocturno] {} completada.", league.getCode());
        Thread.sleep(7_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[Nocturno] Interrumpida.");
        break;
      } catch (Exception e) {
        log.warn("[Nocturno] Error en {}: {}", league.getCode(), e.getMessage());
      }
    }

    log.info("=== Sincronización nocturna completada. ===");
  }
}
