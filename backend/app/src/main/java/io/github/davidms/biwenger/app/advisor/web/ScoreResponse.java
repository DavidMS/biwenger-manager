package io.github.davidms.biwenger.app.advisor.web;

import io.github.davidms.biwenger.app.advisor.domain.PlayerScore;
import io.github.davidms.biwenger.app.advisor.domain.Venue;
import java.math.BigDecimal;

/** Every number of a player's score (ADR 0006), as the advisor endpoints show it. */
public record ScoreResponse(
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

  static ScoreResponse from(PlayerScore score) {
    return new ScoreResponse(
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
