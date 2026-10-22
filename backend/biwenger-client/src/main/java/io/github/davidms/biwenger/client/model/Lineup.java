package io.github.davidms.biwenger.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A saved lineup: formation ({@code "3-4-3"}), starters and reserves by player id with {@code null}
 * for an empty slot, and the squad players left out. It can be stale: a player may have left the
 * squad since it was saved.
 */
public record Lineup(
    String type,
    int points,
    List<@Nullable Long> players,
    List<@Nullable Long> reserves,
    List<Long> discarded,
    @Nullable PlayerRef captain,
    @Nullable PlayerRef coach,
    long date) {

  public Lineup {
    Objects.requireNonNull(type, "type");
    // Unmodifiable but null-tolerant: an empty slot is a null id.
    players = players == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(players));
    reserves =
        reserves == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(reserves));
    discarded = discarded == null ? List.of() : List.copyOf(discarded);
  }
}
