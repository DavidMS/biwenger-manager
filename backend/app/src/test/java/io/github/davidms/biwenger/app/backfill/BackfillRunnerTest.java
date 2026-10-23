package io.github.davidms.biwenger.app.backfill;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.backfill.service.BackfillService;
import io.github.davidms.biwenger.app.biwenger.BiwengerProperties;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class BackfillRunnerTest {

  private final BackfillService backfill = mock(BackfillService.class);

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
    new BackfillRunner(backfill, login("", "")).start();

    verify(backfill, never()).run();
  }

  @Test
  void aFailureIsLoggedNotThrown() {
    when(backfill.run()).thenThrow(new IllegalStateException("database down"));
    var runner = new BackfillRunner(backfill, login("manager@example.com", "pw"));

    assertThatNoException().isThrownBy(runner::run);
    verify(backfill).run();
  }
}
