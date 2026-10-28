package io.github.davidms.biwenger.app.dashboard.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.util.Objects;

/**
 * Your team now: cash, season points, place in a league of {@code managers}, and team value with
 * its change since Biwenger's last update.
 */
public record TeamStatus(
    Money balance, int points, int position, int managers, Money teamValue, Money teamValueChange) {

  public TeamStatus {
    Objects.requireNonNull(balance, "balance");
    Objects.requireNonNull(teamValue, "teamValue");
    Objects.requireNonNull(teamValueChange, "teamValueChange");
    if (position < 1 || position > managers) {
      throw new IllegalArgumentException(
          "Position " + position + " is outside a league of " + managers + " managers");
    }
  }

  /** A negative balance when a round starts scores 0 points that round. */
  public boolean balanceNegative() {
    return balance.isNegative();
  }
}
