package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.player.domain.Position;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How many starters each position has in the XI the advisors reason about, set from {@code
 * biwenger.advisor.formation.*}. Your starters in a position are your best players there by
 * expected points, ties by player id (ADR 0007, ADR 0008).
 */
public record ReferenceFormation(Map<Position, Integer> starters) {

  public static final ReferenceFormation DEFAULT =
      new ReferenceFormation(
          Map.of(
              Position.GOALKEEPER, 1,
              Position.DEFENDER, 4,
              Position.MIDFIELDER, 4,
              Position.FORWARD, 2,
              Position.COACH, 0));

  public ReferenceFormation {
    Objects.requireNonNull(starters, "starters");
    EnumMap<Position, Integer> copy = new EnumMap<>(Position.class);
    for (Position position : Position.values()) {
      Integer count = starters.get(position);
      if (count == null || count < 0) {
        throw new IllegalArgumentException(
            "The formation needs a count of 0 or more for " + position + ": " + count);
      }
      copy.put(position, count);
    }
    // An EnumMap iterates in declaration order, so the order is the same on every run.
    starters = Collections.unmodifiableMap(copy);
  }

  public int startersAt(Position position) {
    return starters.get(position);
  }

  /** Your players in {@code position}, best first: the first {@link #startersAt} start. */
  public static List<ScoredPlayer> depthChart(List<ScoredPlayer> squad, Position position) {
    return squad.stream()
        .filter(s -> s.player().position() == position)
        .sorted(Comparator.comparing(ScoredPlayer::score, PlayerScore.BY_EXPECTED_POINTS))
        .toList();
  }
}
