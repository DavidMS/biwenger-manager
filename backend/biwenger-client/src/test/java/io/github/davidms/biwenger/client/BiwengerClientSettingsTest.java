package io.github.davidms.biwenger.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BiwengerClientSettingsTest {

  @Test
  void backoffDoublesUpToTheMaximum() {
    var settings =
        new BiwengerClientSettings(
            Duration.ZERO, 5, Duration.ofMillis(100), Duration.ofMillis(350));

    assertThat(settings.backoffAfter(1)).isEqualTo(Duration.ofMillis(100));
    assertThat(settings.backoffAfter(2)).isEqualTo(Duration.ofMillis(200));
    assertThat(settings.backoffAfter(3)).isEqualTo(Duration.ofMillis(350));
    assertThat(settings.backoffAfter(50)).isEqualTo(Duration.ofMillis(350));
  }

  @Test
  void defaultsWaitHalfASecondAndTryThreeTimes() {
    var defaults = BiwengerClientSettings.DEFAULTS;

    assertThat(defaults.minRequestInterval()).isEqualTo(Duration.ofMillis(500));
    assertThat(defaults.maxAttempts()).isEqualTo(3);
    assertThat(defaults.backoffAfter(1)).isEqualTo(Duration.ofSeconds(1));
    assertThat(defaults.backoffAfter(2)).isEqualTo(Duration.ofSeconds(2));
  }

  @Test
  void rejectsSettingsThatMakeNoSense() {
    assertThatThrownBy(
            () -> new BiwengerClientSettings(Duration.ZERO, 0, Duration.ZERO, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new BiwengerClientSettings(Duration.ofMillis(-1), 3, Duration.ZERO, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
