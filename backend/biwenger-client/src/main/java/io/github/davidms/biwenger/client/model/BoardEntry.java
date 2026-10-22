package io.github.davidms.biwenger.client.model;

import java.util.List;

/**
 * An entry of {@code GET /api/v2/league/{id}/board}, newest first. {@code type}: {@code "market"}
 * (auction wins) or {@code "transfer"} (sales to the game, clause buys).
 */
public record BoardEntry(String type, long date, List<BoardItem> content) {

  public BoardEntry {
    content = content == null ? List.of() : List.copyOf(content);
  }
}
