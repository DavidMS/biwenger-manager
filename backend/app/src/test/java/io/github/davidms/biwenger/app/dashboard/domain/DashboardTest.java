package io.github.davidms.biwenger.app.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardTest {

  private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

  @Test
  void theNextRoundIsTheFirstToStartAfterNow() {
    var started = new ScheduledRound(1, "Jornada 9", NOW.minusSeconds(3600));
    var later = new ScheduledRound(3, "Jornada 11", NOW.plusSeconds(7 * 86_400));
    var next = new ScheduledRound(2, "Jornada 10", NOW.plusSeconds(86_400));

    assertThat(dashboard(Money.ZERO, List.of(started, later, next)).nextRound(NOW)).contains(next);
  }

  @Test
  void aRoundStartingNowIsNoLongerNext() {
    var startingNow = new ScheduledRound(1, "Jornada 10", NOW);

    assertThat(dashboard(Money.ZERO, List.of(startingNow)).nextRound(NOW)).isEmpty();
  }

  @Test
  void roundsStartingTogetherAreOrderedById() {
    var starts = NOW.plusSeconds(60);
    var second = new ScheduledRound(8, "B", starts);
    var first = new ScheduledRound(5, "A", starts);

    assertThat(dashboard(Money.ZERO, List.of(second, first)).nextRound(NOW)).contains(first);
  }

  @Test
  void noRoundsMeansNoNextRound() {
    assertThat(dashboard(Money.ZERO, List.of()).nextRound(NOW)).isEmpty();
  }

  @Test
  void onlyABalanceBelowZeroIsNegative() {
    assertThat(dashboard(Money.euros(-1), List.of()).balanceNegative()).isTrue();
    assertThat(dashboard(Money.ZERO, List.of()).balanceNegative()).isFalse();
  }

  @Test
  void thePositionIsInsideTheLeague() {
    assertThatThrownBy(() -> new Dashboard(Money.ZERO, 0, 7, 6, Money.ZERO, Money.ZERO, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("league of 6");
    assertThatThrownBy(() -> new Dashboard(Money.ZERO, 0, 0, 6, Money.ZERO, Money.ZERO, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Dashboard dashboard(Money balance, List<ScheduledRound> rounds) {
    return new Dashboard(balance, 220, 6, 6, Money.euros(22_340_000), Money.euros(-70_000), rounds);
  }
}
