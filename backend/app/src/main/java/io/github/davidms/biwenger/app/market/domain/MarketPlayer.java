package io.github.davidms.biwenger.app.market.domain;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * A player on sale today with what the database knows about him: his profile, his value over the
 * last {@link Market#TREND_DAYS} days (oldest first) and his season points.
 */
public record MarketPlayer(
    MarketListing listing, PlayerProfile player, List<DailyPrice> trend, int points) {

  private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

  public MarketPlayer {
    Objects.requireNonNull(listing, "listing");
    Objects.requireNonNull(player, "player");
    if (listing.playerId() != player.id()) {
      throw new IllegalArgumentException(
          "Listing of player " + listing.playerId() + " joined with player " + player.id());
    }
    trend = List.copyOf(trend);
  }

  public Money trendChange() {
    return DailyPrice.change(trend);
  }

  /**
   * Season points per million euros of asking price, to two decimals (half up). Exact, so the same
   * data always gives the same value. Zero for a player listed at no price, which Biwenger doesn't
   * do.
   */
  public BigDecimal pointsPerMillion() {
    long price = listing.price().euros();
    if (price == 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return BigDecimal.valueOf(points)
        .multiply(MILLION)
        .divide(BigDecimal.valueOf(price), 2, RoundingMode.HALF_UP);
  }
}
