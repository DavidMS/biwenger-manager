package io.github.davidms.biwenger.app.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardTest {

  private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");
  private static final TeamStatus TEAM =
      new TeamStatus(Money.ZERO, 220, 6, 6, Money.euros(22_340_000), Money.euros(-70_000));

  @Test
  void theNextRoundIsTheFirstToStartAfterNow() {
    var started = new ScheduledRound(1, "Jornada 9", NOW.minusSeconds(3600));
    var later = new ScheduledRound(3, "Jornada 11", NOW.plusSeconds(7 * 86_400));
    var next = new ScheduledRound(2, "Jornada 10", NOW.plusSeconds(86_400));

    assertThat(new Dashboard(TEAM, List.of(started, later, next)).nextRound(NOW)).contains(next);
  }

  @Test
  void aRoundStartingNowIsNoLongerNext() {
    var startingNow = new ScheduledRound(1, "Jornada 10", NOW);

    assertThat(new Dashboard(TEAM, List.of(startingNow)).nextRound(NOW)).isEmpty();
  }

  @Test
  void roundsStartingTogetherAreOrderedById() {
    var starts = NOW.plusSeconds(60);
    var second = new ScheduledRound(8, "B", starts);
    var first = new ScheduledRound(5, "A", starts);

    assertThat(new Dashboard(TEAM, List.of(second, first)).nextRound(NOW)).contains(first);
  }

  @Test
  void noRoundsMeansNoNextRound() {
    assertThat(new Dashboard(TEAM, List.of()).nextRound(NOW)).isEmpty();
  }
}
