package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.util.Objects;

/** A player on sale, scored, with the listing's asking price. */
public record BuyCandidate(ScoredPlayer player, Money askingPrice) {

  public BuyCandidate {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(askingPrice, "askingPrice");
    if (askingPrice.isNegative()) {
      throw new IllegalArgumentException("An asking price can't be negative: " + askingPrice);
    }
  }
}
