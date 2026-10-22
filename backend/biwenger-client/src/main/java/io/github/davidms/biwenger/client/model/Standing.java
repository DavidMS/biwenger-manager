package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** A manager's place in the standings, team value and current lineup. */
public record Standing(
    long id,
    String name,
    int points,
    int position,
    long teamValue,
    long teamValueInc,
    Lineup lineup) {

  public Standing {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(lineup, "lineup");
  }
}
