package io.github.davidms.biwenger.app.sync;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.biwenger.BiwengerProperties;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.service.DailySyncService;
import io.github.davidms.biwenger.app.sync.service.SyncAlreadyRunningException;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The scheduler's own decisions, without Spring: skip without a login, survive a failure. */
class DailySyncSchedulerTest {

  private final DailySyncService sync = mock(DailySyncService.class);

  private static BiwengerProperties login(String email, String password) {
    return new BiwengerProperties(
        email,
        password,
        URI.create("https://biwenger.as.com"),
        URI.create("https://cf.biwenger.com"),
        Duration.ofSeconds(5),
        Duration.ofSeconds(20),
        Duration.ofMillis(500),
        3,
        Duration.ofSeconds(1),
        Duration.ofSeconds(10));
  }

  @Test
  void skipsWhenNoLoginIsSet() {
    new DailySyncScheduler(sync, login("", "")).run();

    verify(sync, never()).syncToday(SyncTrigger.SCHEDULED);
  }

  @Test
  void runsWhenTheLoginIsSet() {
    new DailySyncScheduler(sync, login("manager@example.com", "pw")).run();

    verify(sync).syncToday(SyncTrigger.SCHEDULED);
  }

  @Test
  void skipsWhenAManualSyncIsRunning() {
    when(sync.syncToday(SyncTrigger.SCHEDULED)).thenThrow(new SyncAlreadyRunningException());
    var scheduler = new DailySyncScheduler(sync, login("manager@example.com", "pw"));

    assertThatNoException().isThrownBy(scheduler::run);
  }

  @Test
  void aFailedSyncIsLoggedNotThrown() {
    when(sync.syncToday(SyncTrigger.SCHEDULED))
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"));

    // A scheduled method that throws would only reach Spring's error handler; tomorrow's run must
    // still happen, so the scheduler logs and returns.
    var scheduler = new DailySyncScheduler(sync, login("manager@example.com", "pw"));

    assertThatNoException().isThrownBy(scheduler::run);
    verify(sync).syncToday(SyncTrigger.SCHEDULED);
  }
}
