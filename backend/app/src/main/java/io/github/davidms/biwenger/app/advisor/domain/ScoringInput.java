package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * What the {@link PlayerScorer} needs to know about one player. The lists may come in any order:
 * prices are sorted by day and matches by date, then match id, so callers that read them latest
 * first (the squad) and oldest first (player detail) get the same score.
 *
 * @param prices his value per day, at most one per day
 * @param seasonPoints season points from the daily snapshot
 * @param nextVenue where his club plays next, {@code UNKNOWN} if that isn't known
 */
public record ScoringInput(
    PlayerProfile player,
    List<DailyPrice> prices,
    int seasonPoints,
    List<MatchResult> matches,
    Venue nextVenue) {

  public ScoringInput {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(prices, "prices");
    Objects.requireNonNull(matches, "matches");
    Objects.requireNonNull(nextVenue, "nextVenue");
    prices = prices.stream().sorted(Comparator.comparing(DailyPrice::day)).toList();
    for (int i = 1; i < prices.size(); i++) {
      if (prices.get(i).day().equals(prices.get(i - 1).day())) {
        throw new IllegalArgumentException(
            "Two prices for player " + player.id() + " on " + prices.get(i).day());
      }
    }
    matches =
        matches.stream()
            .sorted(Comparator.comparing(MatchResult::date).thenComparingLong(MatchResult::matchId))
            .toList();
  }
}
