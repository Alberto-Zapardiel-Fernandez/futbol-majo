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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

/**
 * Planificador automático de sincronización de datos de fútbol.
 *
 * <p>Tres niveles de sincronización:</p>
 * <ol>
 *   <li><b>Arranque:</b> sincroniza ligas sin datos Y ligas con partidos atascados
 *       en IN_PLAY/PAUSED (resultado de un apagado inesperado del servidor).</li>
 *   <li><b>Cada 5 minutos:</b> sincroniza ligas con partidos en curso o a punto
 *       de empezar. La query es robusta: captura IN_PLAY sin importar la fecha.</li>
 *   <li><b>Cada noche a las 3am:</b> sincroniza todas las ligas.</li>
 * </ol>
 */
@Slf4j
@Component
public class SyncScheduler {

  private final FootballDataService footballDataService;
  private final MatchRepository     matchRepository;

  public SyncScheduler(FootballDataService footballDataService,
                       MatchRepository matchRepository) {
    this.footballDataService = footballDataService;
    this.matchRepository     = matchRepository;
  }

  // =========================================================================
  // ARRANQUE: ligas sin datos + ligas con resultados pendientes
  // =========================================================================

  /**
   * Se ejecuta una vez al arrancar la aplicación.
   *
   * <p>Dos pasos en orden:</p>
   * <ol>
   *   <li>Detecta ligas sin ningún partido en BD y las sincroniza.</li>
   *   <li>Detecta ligas con partidos atascados en IN_PLAY/PAUSED
   *       (el servidor estuvo apagado mientras un partido estaba en juego)
   *       y las sincroniza para actualizar el resultado final.</li>
   * </ol>
   *
   * <p>Corre en hilo demonio para no bloquear el servidor HTTP.</p>
   */
  @EventListener(ApplicationReadyEvent.class)
  public void syncOnStartup() {
    Thread thread = new Thread(() -> {
      log.info("=== Sincronización de arranque iniciada ===");

      // ── Paso 1: ligas sin datos ───────────────────────────────────────
      List<String> emptyLeagues = new ArrayList<>();
      for (League league : League.values()) {
        long count = matchRepository.countByCompetitionCode(league.getCode());
        if (count == 0) {
          emptyLeagues.add(league.getCode());
        }
      }

      if (!emptyLeagues.isEmpty()) {
        log.info("[Arranque] Ligas sin datos: {}", emptyLeagues);
        syncCodes(emptyLeagues, "Arranque-nuevas");
      }

      // ── Paso 2: ligas con partidos atascados en IN_PLAY/PAUSED ────────
      // Esto ocurre cuando el servidor se apagó mientras un partido
      // estaba en juego: el resultado final nunca se guardó en BD.
      List<String> stale = matchRepository.findCompetitionsWithStaleLiveMatches();

      if (!stale.isEmpty()) {
        log.info("[Arranque] Ligas con partidos sin actualizar: {} — sincronizando...", stale);
        syncCodes(stale, "Arranque-stale");
      } else {
        log.info("[Arranque] No hay partidos atascados en IN_PLAY/PAUSED.");
      }

      log.info("=== Sincronización de arranque completada ===");

    }, "startup-sync");

    thread.setDaemon(true);
    thread.start();
  }

  // =========================================================================
  // CADA 5 MINUTOS: partidos activos o a punto de empezar
  // =========================================================================

  /**
   * Sincroniza cada 5 minutos las ligas que lo necesitan.
   *
   * <p>La query {@link MatchRepository#findCompetitionsNeedingSync} captura:</p>
   * <ul>
   *   <li>Cualquier partido con estado IN_PLAY o PAUSED en BD
   *       (sin importar la fecha — cubre apagados y reinicios).</li>
   *   <li>Partidos programados en las próximas 2 horas
   *       (para actualizar el estado justo antes del inicio).</li>
   * </ul>
   *
   * <p>{@code fixedDelay}: el temporizador empieza a contar cuando
   * termina la ejecución anterior, evitando solapamientos.</p>
   */
  @Scheduled(fixedDelay = 300_000)
  public void syncLiveAndUpcoming() {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime in2h = now.plusHours(2);

    // LinkedHashSet para mantener orden y eliminar duplicados
    SequencedSet<String> codes = new LinkedHashSet<>(
        matchRepository.findCompetitionsNeedingSync(now, in2h)
    );

    if (codes.isEmpty()) {
      log.debug("[Live] No hay competiciones que sincronizar ahora.");
      return;
    }

    log.info("[Live] Sincronizando {} competición(es): {}", codes.size(), codes);
    syncCodes(new ArrayList<>(codes), "Live");
  }

  // =========================================================================
  // CADA NOCHE A LAS 3AM: todas las ligas
  // =========================================================================

  /**
   * Sincroniza todas las ligas cada noche a las 3:00 AM.
   * Actualiza resultados finales, clasificaciones y estadísticas.
   */
  @Scheduled(cron = "0 0 3 * * *")
  public void nightlySync() {
    log.info("=== Sincronización nocturna iniciada ===");
    List<String> allCodes = java.util.Arrays.stream(League.values())
        .map(League::getCode)
        .toList();
    syncCodes(allCodes, "Nocturno");
    log.info("=== Sincronización nocturna completada ===");
  }

  // =========================================================================
  // Helper privado: sincroniza una lista de códigos respetando el rate limit
  // =========================================================================

  /**
   * Sincroniza una lista de códigos de competición secuencialmente,
   * esperando 7 segundos entre cada uno para respetar el límite de
   * la API gratuita (10 llamadas/minuto).
   *
   * @param codes  Lista de códigos a sincronizar.
   * @param prefix Prefijo para los logs (identifica qué tarea lanzó el sync).
   */
  private void syncCodes(List<String> codes, String prefix) {
    for (String code : codes) {
      try {
        footballDataService.syncAndSaveLaLigaMatches(code);
        log.info("[{}] {} sincronizada correctamente.", prefix, code);

        if (codes.indexOf(code) < codes.size() - 1) {
          Thread.sleep(7_000); // Pausa entre ligas
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[{}] Sincronización interrumpida.", prefix);
        break;
      } catch (Exception e) {
        log.warn("[{}] Error sincronizando {}: {}", prefix, code, e.getMessage());
      }
    }
  }
}
