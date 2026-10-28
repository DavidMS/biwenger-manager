package io.github.davidms.biwenger.app.squad.domain;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import java.util.List;
import java.util.Objects;

/**
 * One of your players: his profile, his value over the last {@link Squad#TREND_DAYS} days (oldest
 * first), his season points and his last {@link Squad#FORM_MATCHES} matches (latest first).
 */
public record SquadPlayer(
    PlayerProfile player, List<DailyPrice> trend, int points, List<MatchResult> form) {

  public SquadPlayer {
    Objects.requireNonNull(player, "player");
    trend = List.copyOf(trend);
    form = List.copyOf(form);
  }

  /** How much his value moved over the trend: last day minus first, zero with fewer than two. */
  public Money trendChange() {
    if (trend.size() < 2) {
      return Money.ZERO;
    }
    return trend.getLast().price().minus(trend.getFirst().price());
  }
}
