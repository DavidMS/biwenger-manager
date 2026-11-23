package io.github.davidms.biwenger.app.advisor.service;

import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import io.github.davidms.biwenger.app.advisor.domain.SellAdvisor;
import io.github.davidms.biwenger.app.advisor.domain.SellCandidate;
import io.github.davidms.biwenger.app.advisor.domain.SellRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.SellWeights;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import io.github.davidms.biwenger.app.player.service.StoredPlayers;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.service.SquadService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Gathers what the {@link SellAdvisor} needs: your squad (read live, then kept a minute by its
 * service) and every match and recent price of your players from the database.
 */
@Service
public class SellAdvisorService {

  private final SquadService squad;
  private final PlayerReader players;
  private final ScoringWeights scoring;
  private final ReferenceFormation formation;
  private final SellWeights selling;
  private final Clock clock;

  SellAdvisorService(
      SquadService squad,
      PlayerReader players,
      ScoringWeights scoring,
      ReferenceFormation formation,
      SellWeights selling,
      Clock clock) {
    this.squad = squad;
    this.players = players;
    this.scoring = scoring;
    this.formation = formation;
    this.selling = selling;
    this.clock = clock;
  }

  public SellRecommendations recommendations() {
    // Biwenger first, outside any transaction: its call can take a while.
    Squad mine = squad.squad();
    List<PlayerProfile> profiles = mine.players().stream().map(p -> p.player()).toList();
    // One day more than the trend: the scorer counts back from the latest stored price, which is
    // yesterday's until the morning sync runs.
    LocalDate pricesFrom = LocalDate.now(clock).minusDays(scoring.trendDays());
    StoredPlayers stored =
        players.read(
            profiles.stream().map(PlayerProfile::id).toList(), pricesFrom, Integer.MAX_VALUE);

    List<SellCandidate> candidates =
        profiles.stream()
            .map(
                player ->
                    new SellCandidate(
                        StoredScores.score(player, stored, scoring),
                        SellCandidate.injuryStreak(
                            stored.lastMatches().getOrDefault(player.id(), List.of()))))
            .toList();
    return new SellRecommendations(
        SellAdvisor.recommend(candidates, mine.missingPlayerIds().size(), formation, selling),
        mine.missingPlayerIds());
  }
}
