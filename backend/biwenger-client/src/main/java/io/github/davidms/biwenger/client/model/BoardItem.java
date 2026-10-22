package io.github.davidms.biwenger.client.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One player moved: who sold ({@code from}), who bought ({@code to}), for how much, and the losing
 * {@code bids} of an auction. {@code type} is {@code "clause"} for a clause buy (inferred meanings
 * in docs/biwenger-api/board.md).
 */
public record BoardItem(
    long player,
    long amount,
    @Nullable Manager from,
    @Nullable Manager to,
    @Nullable String type,
    List<Bid> bids) {

  public BoardItem {
    bids = bids == null ? List.of() : List.copyOf(bids);
  }
}
