package io.github.davidms.biwenger.app.market.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * How a manager owns the player he sells: the release clause, what he paid, when he got him, and
 * from when the clause can be paid. With no lock, that is the day he got him.
 */
public record Ownership(
    Money clause, Money purchasePrice, Instant acquiredAt, Instant clauseUnlocksAt) {

  public Ownership {
    Objects.requireNonNull(clause, "clause");
    Objects.requireNonNull(purchasePrice, "purchasePrice");
    Objects.requireNonNull(acquiredAt, "acquiredAt");
    Objects.requireNonNull(clauseUnlocksAt, "clauseUnlocksAt");
    if (clauseUnlocksAt.isBefore(acquiredAt)) {
      throw new IllegalArgumentException("A clause can't unlock before the player was acquired");
    }
  }

  public boolean clauseLockedAt(Instant time) {
    return time.isBefore(clauseUnlocksAt);
  }
}
