package io.github.davidms.biwenger.app.advisor.service;

import io.github.davidms.biwenger.app.advisor.domain.BuyAdvisor;
import io.github.davidms.biwenger.app.advisor.domain.BuyCandidate;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.advisor.domain.ScoredPlayer;
import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.dashboard.service.DashboardService;
import io.github.davidms.biwenger.app.market.domain.Market;
import io.github.davidms.biwenger.app.market.service.MarketService;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import io.github.davidms.biwenger.app.player.service.StoredPlayers;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.service.SquadService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Gathers what the {@link BuyAdvisor} needs: today's market and your squad (both read live, then
 * kept a minute by their services), your balance (live, through the dashboard's team only) and
 * every match and recent price of those players from the database. The next venue is {@code
 * UNKNOWN} until #97 stores fixtures.
 */
@Service
public class BuyAdvisorService {

  private final MarketService market;
  private final SquadService squad;
  private final DashboardService dashboard;
  private final PlayerReader players;
  private final ScoringWeights scoring;
  private final ReferenceFormation formation;
  private final BuyWeights buying;
  private final Clock clock;

  BuyAdvisorService(
      MarketService market,
      SquadService squad,
      DashboardService dashboard,
      PlayerReader players,
      ScoringWeights scoring,
      ReferenceFormation formation,
      BuyWeights buying,
      Clock clock) {
    this.market = market;
    this.squad = squad;
    this.dashboard = dashboard;
    this.players = players;
    this.scoring = scoring;
    this.formation = formation;
    this.buying = buying;
    this.clock = clock;
  }

  public BuyRecommendations recommendations() {
    // Biwenger first, outside any transaction: its calls can take a while.
    Market onSale = market.market();
    Squad mine = squad.squad();
    Money balance = dashboard.team().balance();

    Set<Long> ids = new LinkedHashSet<>();
    onSale.players().forEach(p -> ids.add(p.player().id()));
    mine.players().forEach(p -> ids.add(p.player().id()));
    // One day more than the trend: the scorer counts back from the latest stored price, which is
    // yesterday's until the morning sync runs.
    LocalDate pricesFrom = LocalDate.now(clock).minusDays(scoring.trendDays());
    StoredPlayers stored = players.read(List.copyOf(ids), pricesFrom, Integer.MAX_VALUE);

    List<BuyCandidate> candidates =
        onSale.players().stream()
            .map(
                p ->
                    new BuyCandidate(
                        StoredScores.score(p.player(), stored, scoring), p.listing().price()))
            .toList();
    List<ScoredPlayer> squadScores =
        mine.players().stream().map(p -> StoredScores.score(p.player(), stored, scoring)).toList();
    return new BuyRecommendations(
        balance,
        BuyAdvisor.recommend(candidates, squadScores, balance, formation, buying),
        mine.missingPlayerIds(),
        onSale.missingPlayerIds());
  }
}
