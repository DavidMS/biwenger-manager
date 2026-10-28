package io.github.davidms.biwenger.app.player.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** A player's value on one day, from Biwenger's price history. */
public record DailyPrice(LocalDate day, Money price) {

  /**
   * How much the value moved over {@code prices}, oldest first: last minus first, zero with fewer
   * than two.
   */
  public static Money change(List<DailyPrice> prices) {
    if (prices.size() < 2) {
      return Money.ZERO;
    }
    return prices.getLast().price().minus(prices.getFirst().price());
  }

  public DailyPrice {
    Objects.requireNonNull(day, "day");
    Objects.requireNonNull(price, "price");
  }
}
