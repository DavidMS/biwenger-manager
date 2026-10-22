package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;

/**
 * One round in a player's {@code fitness}: Biwenger sends the points (a number), a status such as
 * {@code "injured"} or {@code "sanctioned"} (a string), or {@code null} when the player didn't
 * play.
 */
public record Fitness(@Nullable Integer points, @Nullable String status) {

  /** The player didn't play, for no stated reason. */
  public static final Fitness NOT_PLAYED = new Fitness(null, null);

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static Fitness fromJson(Object value) {
    return switch (value) {
      case Number points -> new Fitness(points.intValue(), null);
      case String status -> new Fitness(null, status);
      default -> throw new IllegalArgumentException("Unexpected fitness value: " + value);
    };
  }

  public boolean played() {
    return points != null;
  }
}
