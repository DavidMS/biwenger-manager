package io.github.davidms.biwenger.app.advisor.service;

import io.github.davidms.biwenger.app.advisor.domain.PlayerScorer;
import io.github.davidms.biwenger.app.advisor.domain.ScoredPlayer;
import io.github.davidms.biwenger.app.advisor.domain.ScoringInput;
import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import io.github.davidms.biwenger.app.advisor.domain.Venue;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.service.StoredPlayers;
import java.util.List;

/**
 * Scores a player from what {@code PlayerReader} stored. A player with no stored rows scores from
 * empty history. The next venue is {@code UNKNOWN} until #97 stores fixtures.
 */
final class StoredScores {

  private StoredScores() {}

  static ScoredPlayer score(PlayerProfile player, StoredPlayers stored, ScoringWeights weights) {
    ScoringInput input =
        new ScoringInput(
            player,
            stored.prices().getOrDefault(player.id(), List.of()),
            stored.seasonPoints().getOrDefault(player.id(), 0),
            stored.lastMatches().getOrDefault(player.id(), List.of()),
            Venue.UNKNOWN);
    return new ScoredPlayer(player, PlayerScorer.score(input, weights));
  }
}
