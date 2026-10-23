package io.github.davidms.biwenger.app.market.domain;

import io.github.davidms.biwenger.app.league.domain.Manager;
import java.util.Objects;

/** Who sells a player in the market: the game (a free player) or a manager. */
public sealed interface Seller {

  /** A free player, sold by the game. */
  record Game() implements Seller {}

  record ByManager(Manager manager) implements Seller {

    public ByManager {
      Objects.requireNonNull(manager, "manager");
    }
  }
}
