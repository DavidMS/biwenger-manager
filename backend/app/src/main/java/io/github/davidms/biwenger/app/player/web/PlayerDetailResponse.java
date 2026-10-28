package io.github.davidms.biwenger.app.player.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerDetail;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.Team;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * {@code GET /api/players/{playerId}}. Money is whole euros. {@code prices} is the value per day,
 * oldest first; {@code matches} in the order they were played. {@code teamName} is left out when
 * the club isn't stored yet.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlayerDetailResponse(
    long id,
    String name,
    long teamId,
    String teamName,
    Position position,
    PlayerStatus status,
    long value,
    int points,
    List<PricePoint> prices,
    List<Match> matches) {

  static PlayerDetailResponse from(PlayerDetail detail, Optional<Team> team) {
    var player = detail.player();
    return new PlayerDetailResponse(
        player.id(),
        player.name(),
        player.teamId(),
        team.map(Team::name).orElse(null),
        player.position(),
        player.status(),
        player.price().euros(),
        detail.points(),
        detail.prices().stream().map(PricePoint::from).toList(),
        detail.matches().stream().map(Match::from).toList());
  }

  public record PricePoint(LocalDate day, long price) {

    static PricePoint from(DailyPrice price) {
      return new PricePoint(price.day(), price.price().euros());
    }
  }

  /**
   * One club match: played, with {@code points} and {@code minutes}, or missed, with the {@code
   * reason} ({@code AVAILABLE} means fit but not picked).
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Match(
      long matchId,
      long roundId,
      Instant date,
      boolean home,
      boolean played,
      Integer points,
      Integer minutes,
      PlayerStatus reason) {

    static Match from(MatchResult match) {
      return switch (match.outcome()) {
        case MatchResult.Played(int points, int minutes) ->
            new Match(
                match.matchId(),
                match.roundId(),
                match.date(),
                match.home(),
                true,
                points,
                minutes,
                null);
        case MatchResult.Missed(var reason) ->
            new Match(
                match.matchId(),
                match.roundId(),
                match.date(),
                match.home(),
                false,
                null,
                null,
                reason);
      };
    }
  }
}
