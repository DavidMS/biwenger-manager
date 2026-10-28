package io.github.davidms.biwenger.app.league.domain;

import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import java.util.List;
import java.util.Objects;

/**
 * A manager's squad as his lineup shows it ({@link LeagueEntry}), with what the database knows
 * about each player. {@code missingPlayerIds} are players the daily sync has never stored.
 */
public record RivalSquad(LeagueEntry entry, List<Member> players, List<Long> missingPlayerIds) {

  public RivalSquad {
    Objects.requireNonNull(entry, "entry");
    players = List.copyOf(players);
    missingPlayerIds = List.copyOf(missingPlayerIds);
  }

  /** One player with his season points. */
  public record Member(PlayerProfile player, int points) {

    public Member {
      Objects.requireNonNull(player, "player");
    }
  }
}
