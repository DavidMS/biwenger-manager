package io.github.davidms.biwenger.app.market.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.davidms.biwenger.app.market.domain.Market;
import io.github.davidms.biwenger.app.market.domain.MarketPlayer;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.Team;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /api/market}. Money is whole euros: {@code price} is the asking price, {@code value}
 * the player's value. {@code trend} is the value per day, oldest first.
 */
public record MarketResponse(
    List<Player> players, List<TeamItem> teams, List<Long> missingPlayerIds) {

  static MarketResponse from(Market market) {
    return new MarketResponse(
        market.players().stream().map(Player::from).toList(),
        market.teams().stream().map(TeamItem::from).toList(),
        market.missingPlayerIds());
  }

  public record Player(
      long id,
      String name,
      long teamId,
      Position position,
      PlayerStatus status,
      long price,
      long value,
      long trendChange,
      List<PricePoint> trend,
      int points,
      BigDecimal pointsPerMillion,
      Instant closesAt,
      Seller seller) {

    static Player from(MarketPlayer marketPlayer) {
      var listing = marketPlayer.listing();
      var player = marketPlayer.player();
      return new Player(
          player.id(),
          player.name(),
          player.teamId(),
          player.position(),
          player.status(),
          listing.price().euros(),
          player.price().euros(),
          marketPlayer.trendChange().euros(),
          marketPlayer.trend().stream().map(PricePoint::from).toList(),
          marketPlayer.points(),
          marketPlayer.pointsPerMillion(),
          listing.closesAt(),
          Seller.from(listing.seller()));
    }
  }

  public record PricePoint(LocalDate day, long price) {

    static PricePoint from(DailyPrice price) {
      return new PricePoint(price.day(), price.price().euros());
    }
  }

  /** A free player ({@code free}, nothing else) or a manager's sale, with the manager. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Seller(boolean free, Long managerId, String managerName) {

    static Seller from(io.github.davidms.biwenger.app.market.domain.Seller seller) {
      return switch (seller) {
        case io.github.davidms.biwenger.app.market.domain.Seller.Game _ ->
            new Seller(true, null, null);
        case io.github.davidms.biwenger.app.market.domain.Seller.ByManager(var manager, var _) ->
            new Seller(false, manager.id(), manager.name());
      };
    }
  }

  public record TeamItem(long id, String name) {

    static TeamItem from(Team team) {
      return new TeamItem(team.id(), team.name());
    }
  }
}
