package io.github.davidms.biwenger.client;

import java.time.Duration;

/** Settings for tests that aren't about timing: retries still happen, but nothing waits. */
final class TestSettings {

  static final BiwengerClientSettings NO_WAITING =
      new BiwengerClientSettings(Duration.ZERO, 3, Duration.ZERO, Duration.ZERO);

  private TestSettings() {}
}
