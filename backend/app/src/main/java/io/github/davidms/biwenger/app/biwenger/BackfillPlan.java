package io.github.davidms.biwenger.app.biwenger;

import java.util.List;
import java.util.Objects;

/**
 * What the backfill has to do: the season, the league's scoring system, and every catalog player,
 * ordered by id.
 */
public record BackfillPlan(String season, int scoreId, List<PlayerRef> players) {

  public BackfillPlan {
    Objects.requireNonNull(season, "season");
    players = List.copyOf(players);
  }

  /** A player by id, and the slug the CDN needs. */
  public record PlayerRef(long id, String slug) {}
}
