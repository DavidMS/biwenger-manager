package io.github.davidms.biwenger.app.player.domain;

import java.util.List;
import java.util.Objects;

/**
 * Everything stored about one player: his profile, his value per day (oldest first), his season
 * points and his matches (in the order they were played).
 */
public record PlayerDetail(
    PlayerProfile player, List<DailyPrice> prices, int points, List<MatchResult> matches) {

  public PlayerDetail {
    Objects.requireNonNull(player, "player");
    prices = List.copyOf(prices);
    matches = List.copyOf(matches);
  }
}
