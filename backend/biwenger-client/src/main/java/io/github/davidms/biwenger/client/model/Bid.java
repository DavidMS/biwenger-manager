package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** A losing bid in an auction, visible on the board. */
public record Bid(Manager user, long amount) {

  public Bid {
    Objects.requireNonNull(user, "user");
  }
}
