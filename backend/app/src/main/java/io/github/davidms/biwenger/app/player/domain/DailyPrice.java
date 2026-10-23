package io.github.davidms.biwenger.app.player.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.time.LocalDate;
import java.util.Objects;

/** A player's value on one day, from Biwenger's price history. */
public record DailyPrice(LocalDate day, Money price) {

  public DailyPrice {
    Objects.requireNonNull(day, "day");
    Objects.requireNonNull(price, "price");
  }
}
