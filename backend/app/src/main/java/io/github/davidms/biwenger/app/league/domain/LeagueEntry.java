package io.github.davidms.biwenger.app.league.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A manager's row in the league table, with his squad as his saved lineup shows it: starters,
 * reserves, the players left out and the coach. It is inferred and can be stale, since a player
 * bought or sold after {@code lineupSavedAt} isn't reflected. Player ids are sorted.
 */
public record LeagueEntry(Standing standing, List<Long> squadPlayerIds, Instant lineupSavedAt) {

  public LeagueEntry {
    Objects.requireNonNull(standing, "standing");
    Objects.requireNonNull(lineupSavedAt, "lineupSavedAt");
    squadPlayerIds = squadPlayerIds.stream().distinct().sorted().toList();
  }

  public long managerId() {
    return standing.manager().id();
  }
}
