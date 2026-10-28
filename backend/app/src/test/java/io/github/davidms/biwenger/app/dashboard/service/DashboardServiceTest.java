package io.github.davidms.biwenger.app.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.dashboard.domain.Dashboard;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

  private static final Dashboard DASHBOARD =
      new Dashboard(
          Money.euros(51_420_000),
          220,
          6,
          6,
          Money.euros(22_340_000),
          Money.euros(-70_000),
          List.of());

  private final BiwengerGateway biwenger = mock(BiwengerGateway.class);
  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-10-28T10:00:00Z"), ZoneId.of("Europe/Madrid"));
  private final DashboardService dashboards = new DashboardService(biwenger, clock);

  @Test
  void readsBiwengerOncePerMinute() {
    when(biwenger.dashboard()).thenReturn(DASHBOARD);

    dashboards.dashboard();
    clock.advance(Duration.ofSeconds(59));
    dashboards.dashboard();

    verify(biwenger, times(1)).dashboard();
    clock.advance(Duration.ofSeconds(1));
    assertThat(dashboards.dashboard()).isEqualTo(DASHBOARD);
    verify(biwenger, times(2)).dashboard();
  }

  @Test
  void aFailureIsNotKept() {
    when(biwenger.dashboard())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(DASHBOARD);

    assertThatThrownBy(dashboards::dashboard).isInstanceOf(BiwengerApiException.class);
    assertThat(dashboards.dashboard()).isEqualTo(DASHBOARD);
  }
}
