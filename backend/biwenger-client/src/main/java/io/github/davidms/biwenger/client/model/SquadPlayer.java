package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** A player in your squad: its id (the catalog key) and how you own it. */
public record SquadPlayer(long id, Ownership owner) {

  public SquadPlayer {
    Objects.requireNonNull(owner, "owner");
  }
}
