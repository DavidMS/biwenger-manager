package io.github.davidms.biwenger.app.advisor.domain;

import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import java.util.Objects;

/** A player with his score from the {@link PlayerScorer}. */
public record ScoredPlayer(PlayerProfile player, PlayerScore score) {

  public ScoredPlayer {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(score, "score");
    if (player.id() != score.playerId()) {
      throw new IllegalArgumentException(
          "Player " + player.id() + " joined with the score of player " + score.playerId());
    }
  }
}
