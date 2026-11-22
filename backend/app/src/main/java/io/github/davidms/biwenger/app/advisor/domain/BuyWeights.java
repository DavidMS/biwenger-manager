package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.player.domain.Position;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The tunable numbers of the {@link BuyAdvisor}, set from {@code biwenger.advisor.buy.*}.
 *
 * @param starters how many starters each position has in the reference formation. A candidate's
 *     gain is measured against the weakest of them; a position with 0 starters is never suggested.
 * @param limit the most recommendations returned
 */
public record BuyWeights(Map<Position, Integer> starters, int limit) {

  public static final BuyWeights DEFAULTS =
      new BuyWeights(
          Map.of(
              Position.GOALKEEPER, 1,
              Position.DEFENDER, 4,
              Position.MIDFIELDER, 4,
              Position.FORWARD, 2,
              Position.COACH, 0),
          10);

  public BuyWeights {
    Objects.requireNonNull(starters, "starters");
    EnumMap<Position, Integer> copy = new EnumMap<>(Position.class);
    for (Position position : Position.values()) {
      Integer count = starters.get(position);
      if (count == null || count < 0) {
        throw new IllegalArgumentException(
            "starters needs a count of 0 or more for " + position + ": " + count);
      }
      copy.put(position, count);
    }
    // An EnumMap iterates in declaration order, so the order is the same on every run.
    starters = Collections.unmodifiableMap(copy);
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be at least 1: " + limit);
    }
  }

  public int startersAt(Position position) {
    return starters.get(position);
  }
}
