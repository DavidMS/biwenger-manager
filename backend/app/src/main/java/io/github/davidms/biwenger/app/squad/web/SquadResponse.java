package io.github.davidms.biwenger.app.squad.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.domain.SquadPlayer;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /api/squad}. Money is whole euros. {@code trend} is the value per day, oldest first;
 * {@code form} the last matches, latest first.
 */
public record SquadResponse(List<Player> players, List<Long> missingPlayerIds) {

  static SquadResponse from(Squad squad) {
    return new SquadResponse(
        squad.players().stream().map(Player::from).toList(), squad.missingPlayerIds());
  }

  public record Player(
      long id,
      String name,
      String slug,
      long teamId,
      Position position,
      PlayerStatus status,
      long price,
      long trendChange,
      List<PricePoint> trend,
      int points,
      List<Match> form) {

    static Player from(SquadPlayer squadPlayer) {
      var player = squadPlayer.player();
      return new Player(
          player.id(),
          player.name(),
          player.slug(),
          player.teamId(),
          player.position(),
          player.status(),
          player.price().euros(),
          squadPlayer.trendChange().euros(),
          squadPlayer.trend().stream().map(PricePoint::from).toList(),
          squadPlayer.points(),
          squadPlayer.form().stream().map(Match::from).toList());
    }
  }

  public record PricePoint(LocalDate day, long price) {

    static PricePoint from(DailyPrice price) {
      return new PricePoint(price.day(), price.price().euros());
    }
  }

  /** A match he played, with {@code points}, or missed, with the {@code reason}. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Match(boolean played, Integer points, PlayerStatus reason) {

    static Match from(MatchResult match) {
      return switch (match.outcome()) {
        case MatchResult.Played(int points, int _) -> new Match(true, points, null);
        case MatchResult.Missed(var reason) -> new Match(false, null, reason);
      };
    }
  }
}
