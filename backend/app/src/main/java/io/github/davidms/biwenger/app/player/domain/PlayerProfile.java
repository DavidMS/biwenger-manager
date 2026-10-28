package io.github.davidms.biwenger.app.player.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.util.Objects;

/** A player as the latest daily sync stored him: who he is, his status and his value. */
public record PlayerProfile(
    long id,
    String name,
    String slug,
    long teamId,
    Position position,
    PlayerStatus status,
    Money price) {

  public PlayerProfile {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(price, "price");
    if (price.isNegative()) {
      throw new IllegalArgumentException("A player's price can't be negative: " + price);
    }
  }
}
