package io.github.davidms.biwenger.app.sync;

import io.github.davidms.biwenger.app.biwenger.BiwengerProperties;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.service.DailySyncService;
import io.github.davidms.biwenger.app.sync.service.SyncAlreadyRunningException;
import java.lang.System.Logger.Level;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the daily sync at {@code biwenger.sync.cron}, by default 07:30 Madrid time, half an hour
 * after the market closes. {@code biwenger.sync.enabled=false} turns it off.
 */
@Component
@ConditionalOnProperty(name = "biwenger.sync.enabled", havingValue = "true", matchIfMissing = true)
class DailySyncScheduler {

  static final String CRON = "${biwenger.sync.cron:0 30 7 * * *}";

  private static final System.Logger LOG = System.getLogger(DailySyncScheduler.class.getName());

  private final DailySyncService sync;
  private final BiwengerProperties properties;

  DailySyncScheduler(DailySyncService sync, BiwengerProperties properties) {
    this.sync = sync;
    this.properties = properties;
  }

  @Scheduled(cron = CRON, zone = "Europe/Madrid")
  void run() {
    if (isBlank(properties.email()) || isBlank(properties.password())) {
      LOG.log(
          Level.INFO, "Skipping the daily sync: BIWENGER_EMAIL and BIWENGER_PASSWORD are not set");
      return;
    }
    try {
      sync.syncToday(SyncTrigger.SCHEDULED);
    } catch (SyncAlreadyRunningException e) {
      LOG.log(Level.INFO, "Skipping the daily sync: a manual sync is running");
    } catch (RuntimeException e) {
      // The service recorded and logged it; tomorrow's run tries again.
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
