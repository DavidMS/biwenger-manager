package io.github.davidms.biwenger.app.advisor.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.EmptySlot;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendation.Replaces;
import io.github.davidms.biwenger.app.advisor.domain.BuyRecommendations;
import io.github.davidms.biwenger.app.advisor.domain.PlayerScore;
import io.github.davidms.biwenger.app.advisor.domain.Venue;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code GET /api/advisor/buys}. Money is whole euros. Each recommendation carries the player's
 * full {@link ScoreItem score}, so the explanation can be checked against the numbers. The missing
 * ids are players the advisor couldn't score because the daily sync never stored them.
 */
public record BuyRecommendationsResponse(
    long balance,
    List<Recommendation> recommendations,
    List<Long> missingSquadPlayerIds,
    List<Long> missingMarketPlayerIds) {

  static BuyRecommendationsResponse from(BuyRecommendations buys) {
    return new BuyRecommendationsResponse(
        buys.balance().euros(),
        buys.recommendations().stream().map(Recommendation::from).toList(),
        buys.missingSquadPlayerIds(),
        buys.missingMarketPlayerIds());
  }

  /** {@code replaces} is left out when he fills an empty place in your XI. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Recommendation(
      long id,
      String name,
      long teamId,
      Position position,
      PlayerStatus status,
      long askingPrice,
      long value,
      BigDecimal gain,
      BigDecimal gainPerMillion,
      long budgetLeft,
      Starter replaces,
      ScoreItem score,
      String explanation) {

    static Recommendation from(BuyRecommendation buy) {
      var player = buy.candidate().player();
      Starter replaces =
          switch (buy.slot()) {
            case EmptySlot _ -> null;
            case Replaces(var starter) ->
                new Starter(
                    starter.player().id(),
                    starter.player().name(),
                    starter.score().expectedPoints());
          };
      return new Recommendation(
          player.id(),
          player.name(),
          player.teamId(),
          player.position(),
          player.status(),
          buy.askingPrice().euros(),
          player.price().euros(),
          buy.gain(),
          buy.gainPerMillion(),
          buy.budgetLeft().euros(),
          replaces,
          ScoreItem.from(buy.candidate().score()),
          buy.explanation());
    }
  }

  /** The starter of yours he would replace. */
  public record Starter(long id, String name, BigDecimal expectedPoints) {}

  /** Every number of the player's score (ADR 0006). */
  public record ScoreItem(
      BigDecimal expectedPoints,
      BigDecimal recentForm,
      int formMatchesUsed,
      BigDecimal seasonAverage,
      int gamesPlayed,
      BigDecimal base,
      Venue venue,
      int homeGames,
      int awayGames,
      BigDecimal venueAverage,
      BigDecimal venueFactor,
      BigDecimal statusMultiplier,
      long valueTrend,
      BigDecimal valueTrendPercent,
      int trendDays,
      BigDecimal pointsPerMillion) {

    static ScoreItem from(PlayerScore score) {
      return new ScoreItem(
          score.expectedPoints(),
          score.recentForm(),
          score.formMatchesUsed(),
          score.seasonAverage(),
          score.gamesPlayed(),
          score.base(),
          score.venue(),
          score.homeGames(),
          score.awayGames(),
          score.venueAverage(),
          score.venueFactor(),
          score.statusMultiplier(),
          score.valueTrend().euros(),
          score.valueTrendPercent(),
          score.trendDays(),
          score.pointsPerMillion());
    }
  }
}
