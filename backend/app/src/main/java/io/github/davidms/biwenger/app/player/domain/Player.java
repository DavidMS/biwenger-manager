package io.github.davidms.biwenger.app.player.domain;

import io.github.davidms.biwenger.app.common.Money;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A La Liga player as the app sees him. Points are in the league's scoring system. {@code form} is
 * the last rounds, most recent first. {@code statusNote} explains the status, and is empty when he
 * is available.
 */
public record Player(
    long id,
    String name,
    String slug,
    long teamId,
    Position position,
    Set<Position> otherPositions,
    Money price,
    Money priceChange,
    PlayerStatus status,
    String statusNote,
    List<RoundForm> form,
    int points,
    int pointsHome,
    int pointsAway,
    int gamesHome,
    int gamesAway) {

  public Player {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(price, "price");
    Objects.requireNonNull(priceChange, "priceChange");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(statusNote, "statusNote");
    // An EnumSet iterates in declaration order, so the order is the same on every run.
    otherPositions =
        otherPositions.isEmpty()
            ? Set.of()
            : Collections.unmodifiableSet(EnumSet.copyOf(otherPositions));
    form = List.copyOf(form);
    if (price.isNegative()) {
      throw new IllegalArgumentException("A player's price can't be negative: " + price);
    }
    if (gamesHome < 0 || gamesAway < 0) {
      throw new IllegalArgumentException("Games played can't be negative");
    }
  }

  public int gamesPlayed() {
    return gamesHome + gamesAway;
  }

  /** Whether he can be lined up in this position: his own, or one of the others he's allowed. */
  public boolean canPlay(Position slot) {
    return position == slot || otherPositions.contains(slot);
  }
}
