package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import java.util.List;

/** One player's price history and his matches this season, from the CDN. */
public record PlayerHistory(List<DailyPrice> prices, List<MatchResult> matches) {

  public PlayerHistory {
    prices = List.copyOf(prices);
    matches = List.copyOf(matches);
  }
}
