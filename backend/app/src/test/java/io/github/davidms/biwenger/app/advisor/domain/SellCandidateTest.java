package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.MatchResult.Missed;
import io.github.davidms.biwenger.app.player.domain.MatchResult.Played;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SellCandidateTest {

  private static final Instant START = Instant.parse("2026-08-15T19:00:00Z");

  private static MatchResult match(int week, MatchResult.Outcome outcome) {
    return new MatchResult(100 + week, week, START.plus(Duration.ofDays(7L * week)), true, outcome);
  }

  @Test
  void countsTheLatestMatchesInARowMissedThroughInjury() {
    List<MatchResult> matches =
        List.of(
            match(0, new Missed(PlayerStatus.INJURED)),
            match(1, new Played(6, 90)),
            match(2, new Missed(PlayerStatus.INJURED)),
            match(3, new Missed(PlayerStatus.INJURED)));

    assertThat(SellCandidate.injuryStreak(matches)).isEqualTo(2);
    assertThat(SellCandidate.injuryStreak(matches.reversed())).isEqualTo(2);
  }

  @Test
  void stopsAtAnyOtherReason() {
    assertThat(
            SellCandidate.injuryStreak(
                List.of(
                    match(0, new Missed(PlayerStatus.INJURED)),
                    match(1, new Missed(PlayerStatus.SUSPENDED)))))
        .isZero();
    assertThat(SellCandidate.injuryStreak(List.of())).isZero();
  }
}
