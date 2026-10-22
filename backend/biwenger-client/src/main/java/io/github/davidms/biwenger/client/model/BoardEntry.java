package io.github.davidms.biwenger.client.model;

import java.util.List;
import java.util.Objects;

/**
 * An entry of {@code GET /api/v2/league/{id}/board}, newest first. {@code type}: {@code "market"}
 * (auction wins) or {@code "transfer"} (sales to the game, clause buys).
 */
public record BoardEntry(String type, long date, List<BoardItem> content) {

  public BoardEntry {
    Objects.requireNonNull(type, "type");
    content = content == null ? List.of() : List.copyOf(content);
  }
}
