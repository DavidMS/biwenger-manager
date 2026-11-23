package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * One of your players, scored, with how many of his latest matches in a row he missed through
 * injury.
 */
public record SellCandidate(ScoredPlayer player, int injuryStreak) {

  public SellCandidate {
    Objects.requireNonNull(player, "player");
    if (injuryStreak < 0) {
      throw new IllegalArgumentException("An injury streak can't be negative: " + injuryStreak);
    }
  }

  /**
   * How many of his latest matches in a row he missed through injury, in any input order: matches
   * are taken latest first by date, then match id.
   */
  public static int injuryStreak(List<MatchResult> matches) {
    List<MatchResult> latestFirst =
        matches.stream()
            .sorted(
                Comparator.comparing(MatchResult::date)
                    .thenComparingLong(MatchResult::matchId)
                    .reversed())
            .toList();
    int streak = 0;
    for (MatchResult match : latestFirst) {
      if (match.outcome() instanceof MatchResult.Missed(var reason)
          && reason == PlayerStatus.INJURED) {
        streak++;
      } else {
        break;
      }
    }
    return streak;
  }
}
