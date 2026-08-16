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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

/**
 * Scheduler de sincronización automática.
 * - Arranque: ligas sin datos + partidos atascados en IN_PLAY.
 * - Cada 5 min: ligas con partidos activos o recién comenzados.
 * - Cada noche a las 3am: todas las ligas.
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

  @EventListener(ApplicationReadyEvent.class)
  public void syncOnStartup() {
    Thread thread = new Thread(() -> {
      log.info("=== Sync de arranque iniciado ===");

      List<String> empty = new ArrayList<>();
      for (League league : League.values()) {
        if (matchRepository.countByCompetitionCode(league.getCode()) == 0) {
          empty.add(league.getCode());
        }
      }
      if (!empty.isEmpty()) syncCodes(empty, "Arranque-nuevas");

      List<String> stale = matchRepository.findCompetitionsWithStaleLiveMatches();
      if (!stale.isEmpty()) {
        log.info("[Arranque] Partidos atascados en IN_PLAY: {} — sincronizando...", stale);
        syncCodes(stale, "Arranque-stale");
      }

      log.info("=== Sync de arranque completado ===");
    }, "startup-sync");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Cada 5 minutos sincroniza las ligas que necesitan actualización.
   * La ventana incluye 3 horas hacia el PASADO para capturar partidos que
   * ya empezaron pero siguen con status TIMED en BD (bug de las 19:00 → 19:15).
   */
  @Scheduled(fixedDelay = 120_000)
  public void syncLiveAndUpcoming() {
    // ← CLAVE DEL FIX: minusHours(3) en lugar de "now"
    // Así capturamos partidos que empezaron hasta 3h antes pero no se actualizaron
    OffsetDateTime windowStart = OffsetDateTime.now().minusHours(3);
    OffsetDateTime windowEnd   = OffsetDateTime.now().plusHours(2);

    SequencedSet<String> codes = new LinkedHashSet<>(
        matchRepository.findCompetitionsNeedingSync(windowStart, windowEnd)
    );

    if (codes.isEmpty()) {
      log.debug("[Live] No hay competiciones que sincronizar.");
      return;
    }

    log.info("[Live] Sincronizando {} competición(es): {}", codes.size(), codes);
    syncCodes(new ArrayList<>(codes), "Live");
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void nightlySync() {
    log.info("=== Sync nocturno iniciado ===");
    List<String> all = Arrays.stream(League.values()).map(League::getCode).toList();
    syncCodes(all, "Nocturno");
    log.info("=== Sync nocturno completado ===");
  }

  private void syncCodes(List<String> codes, String prefix) {
    for (int i = 0; i < codes.size(); i++) {
      String code = codes.get(i);
      try {
        footballDataService.syncAndSaveLaLigaMatches(code);
        log.info("[{}] {} OK.", prefix, code);
        if (i < codes.size() - 1) Thread.sleep(7_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.warn("[{}] Error en {}: {}", prefix, code, e.getMessage());
      }
    }
  }
}