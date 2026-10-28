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
import io.github.davidms.biwenger.app.dashboard.domain.TeamStatus;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");
  private static final TeamStatus TEAM =
      new TeamStatus(
          Money.euros(51_420_000), 220, 6, 6, Money.euros(22_340_000), Money.euros(-70_000));
  private static final ScheduledRound IN_TWO_HOURS =
      new ScheduledRound(4908, "Jornada 10", NOW.plus(Duration.ofHours(2)));
  private static final ScheduledRound NEXT_WEEK =
      new ScheduledRound(4909, "Jornada 11", NOW.plus(Duration.ofDays(7)));

  private final BiwengerGateway biwenger = mock(BiwengerGateway.class);
  private final MutableClock clock = new MutableClock(NOW, ZoneId.of("Europe/Madrid"));
  private final DashboardService dashboards = new DashboardService(biwenger, clock);

  @Test
  void readsTheTeamOncePerMinute() {
    when(biwenger.teamStatus()).thenReturn(TEAM);
    when(biwenger.scheduledRounds()).thenReturn(List.of(NEXT_WEEK));

    dashboards.dashboard();
    clock.advance(Duration.ofSeconds(59));
    dashboards.dashboard();

    verify(biwenger, times(1)).teamStatus();
    clock.advance(Duration.ofSeconds(1));
    assertThat(dashboards.dashboard()).isEqualTo(new Dashboard(TEAM, List.of(NEXT_WEEK)));
    verify(biwenger, times(2)).teamStatus();
    verify(biwenger, times(1)).scheduledRounds();
  }

  @Test
  void readsTheScheduleAtMostEverySixHours() {
    when(biwenger.teamStatus()).thenReturn(TEAM);
    when(biwenger.scheduledRounds()).thenReturn(List.of(NEXT_WEEK));

    dashboards.dashboard();
    clock.advance(Duration.ofHours(6).minusSeconds(1));
    dashboards.dashboard();

    verify(biwenger, times(1)).scheduledRounds();
    clock.advance(Duration.ofSeconds(1));
    dashboards.dashboard();
    verify(biwenger, times(2)).scheduledRounds();
  }

  @Test
  void readsTheScheduleAgainOnceTheNextRoundStarts() {
    when(biwenger.teamStatus()).thenReturn(TEAM);
    when(biwenger.scheduledRounds())
        .thenReturn(List.of(IN_TWO_HOURS))
        .thenReturn(List.of(NEXT_WEEK));

    dashboards.dashboard();
    clock.advance(Duration.ofHours(2));

    assertThat(dashboards.dashboard().nextRound(clock.instant())).contains(NEXT_WEEK);
    verify(biwenger, times(2)).scheduledRounds();
  }

  @Test
  void aFailureIsNotKept() {
    when(biwenger.teamStatus())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(TEAM);
    when(biwenger.scheduledRounds())
        .thenThrow(new BiwengerApiException("Biwenger answered HTTP 503"))
        .thenReturn(List.of(NEXT_WEEK));

    assertThatThrownBy(dashboards::dashboard).isInstanceOf(BiwengerApiException.class);
    assertThatThrownBy(dashboards::dashboard).isInstanceOf(BiwengerApiException.class);
    assertThat(dashboards.dashboard()).isEqualTo(new Dashboard(TEAM, List.of(NEXT_WEEK)));
  }
}
