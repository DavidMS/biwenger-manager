package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** Your team in a league. {@code balance} below zero when a round starts scores 0 points. */
public record LeagueMember(long id, String name, long balance, int points, int position) {

  public LeagueMember {
    Objects.requireNonNull(name, "name");
  }
}
