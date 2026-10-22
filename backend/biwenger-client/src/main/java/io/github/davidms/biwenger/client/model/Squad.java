package io.github.davidms.biwenger.client.model;

import java.util.List;

/**
 * {@code GET /api/v2/user?fields=players(id,owner)}: your players. A player can be missing from the
 * catalog (seen for players who left La Liga).
 */
public record Squad(List<SquadPlayer> players) {

  public Squad {
    players = players == null ? List.of() : List.copyOf(players);
  }
}
