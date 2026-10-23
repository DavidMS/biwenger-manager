package io.github.davidms.biwenger.app.market.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.time.Instant;
import java.util.Objects;

/** A player on sale today: who sells him, the asking price, and when bidding closes. */
public record MarketListing(
    long playerId, Seller seller, Money price, Instant listedAt, Instant closesAt) {

  public MarketListing {
    Objects.requireNonNull(seller, "seller");
    Objects.requireNonNull(price, "price");
    Objects.requireNonNull(listedAt, "listedAt");
    Objects.requireNonNull(closesAt, "closesAt");
    if (closesAt.isBefore(listedAt)) {
      throw new IllegalArgumentException("A listing can't close before it opens");
    }
  }

  public boolean isFreePlayer() {
    return seller instanceof Seller.Game;
  }
}
