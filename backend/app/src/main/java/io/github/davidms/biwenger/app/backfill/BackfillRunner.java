package io.github.davidms.biwenger.app.backfill;

import io.github.davidms.biwenger.app.backfill.service.BackfillService;
import io.github.davidms.biwenger.app.biwenger.BiwengerProperties;
import java.lang.System.Logger.Level;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the backfill after startup when {@code biwenger.backfill.enabled=true}, on its own thread
 * so the app is usable meanwhile. A finished backfill does nothing, so the setting can stay on.
 */
@Component
@ConditionalOnProperty(name = "biwenger.backfill.enabled", havingValue = "true")
class BackfillRunner {

  private static final System.Logger LOG = System.getLogger(BackfillRunner.class.getName());

  private final BackfillService backfill;
  private final BiwengerProperties properties;

  BackfillRunner(BackfillService backfill, BiwengerProperties properties) {
    this.backfill = backfill;
    this.properties = properties;
  }

  @EventListener(ApplicationReadyEvent.class)
  void start() {
    if (isBlank(properties.email()) || isBlank(properties.password())) {
      LOG.log(
          Level.INFO, "Skipping the backfill: BIWENGER_EMAIL and BIWENGER_PASSWORD are not set");
      return;
    }
    Thread.ofVirtual().name("backfill").start(this::run);
  }

  void run() {
    try {
      backfill.run();
    } catch (RuntimeException e) {
      // Logged once with the reason; the next startup resumes where this one stopped.
      LOG.log(Level.ERROR, "Backfill failed: " + e.getMessage(), e);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
