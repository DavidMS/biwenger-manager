package io.github.davidms.biwenger.client.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * A player's report for one club match. When he played it has {@code points} (one entry per scoring
 * system, keyed by score id) and {@code rawStats}. When he didn't, both are absent and {@code
 * status} says why: {@code injured}, {@code doubt}, {@code sanctioned}, or {@code ok} when he was
 * fit but not picked.
 */
public record MatchReport(
    Match match,
    boolean home,
    Map<Integer, Integer> points,
    @Nullable RawStats rawStats,
    @Nullable ReportStatus status) {

  public MatchReport {
    Objects.requireNonNull(match, "match");
    // Sorted by score id, so iterating is deterministic.
    points = points == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(points));
    // Points and stats come together, only when he played (docs/biwenger-api/player-reports.md).
    if (points.isEmpty() != (rawStats == null)) {
      throw new IllegalArgumentException("A report must have both points and rawStats, or neither");
    }
  }

  public boolean played() {
    return rawStats != null;
  }

  /** Points in the given scoring system, empty if he didn't play. */
  public OptionalInt points(int scoreId) {
    Integer value = points.get(scoreId);
    return value == null ? OptionalInt.empty() : OptionalInt.of(value);
  }

  public int minutesPlayed() {
    return rawStats == null ? 0 : rawStats.minutesPlayed();
  }
}
