package io.github.davidms.biwenger.app.league.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.util.Objects;

/** A manager's place in the league table, with his points and team value. */
public record Standing(
    Manager manager, int position, int points, Money teamValue, Money teamValueChange) {

  public Standing {
    Objects.requireNonNull(manager, "manager");
    Objects.requireNonNull(teamValue, "teamValue");
    Objects.requireNonNull(teamValueChange, "teamValueChange");
    if (position < 1) {
      throw new IllegalArgumentException("Positions start at 1, but is " + position);
    }
  }
}
