package io.github.davidms.biwenger.app.league.domain;

import java.util.Objects;

/**
 * A round ("Jornada 6"). {@code part} is 1 for a normal round; a postponed round is played later as
 * a separate round with the same {@code shortName} and part 2, 3, …
 */
public record Round(long id, String name, String shortName, RoundStatus status, int part) {

  public Round {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(shortName, "shortName");
    Objects.requireNonNull(status, "status");
    if (part < 1) {
      throw new IllegalArgumentException("A round's part starts at 1, but is " + part);
    }
  }

  public boolean isPostponed() {
    return part > 1;
  }
}
