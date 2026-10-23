package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.League;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.league.domain.Round;
import io.github.davidms.biwenger.app.league.domain.RoundStatus;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.Seller;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.RoundForm;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.CatalogPlayer;
import io.github.davidms.biwenger.client.model.Fitness;
import io.github.davidms.biwenger.client.model.Market;
import io.github.davidms.biwenger.client.model.Sale;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The one place where Biwenger's records become domain types (ADR 0003): codes such as position
 * {@code 4} or status {@code "doubt"} are translated here, Unix seconds become {@link Instant}s and
 * euros become {@link Money}. Pure functions, so the same input always gives the same output.
 *
 * <p>Mapping methods are public; the translations of single codes are package-private. Unknown
 * codes become {@code UNKNOWN}, except a position: a player in the wrong lineup slot is unsafe, so
 * that fails.
 */
public final class BiwengerMapper {

  private BiwengerMapper() {}

  /** Every catalog player, ordered by id. */
  public static List<Player> players(Catalog catalog) {
    return catalog.players().values().stream()
        .map(BiwengerMapper::player)
        .sorted(Comparator.comparingLong(Player::id))
        .toList();
  }

  public static Player player(CatalogPlayer player) {
    return new Player(
        player.id(),
        player.name(),
        player.slug(),
        player.teamId(),
        position(player.position()),
        player.altPositions().stream()
            .map(BiwengerMapper::position)
            .collect(Collectors.toUnmodifiableSet()),
        Money.euros(player.price()),
        Money.euros(player.priceIncrement()),
        status(player.status()),
        player.statusInfo() == null ? "" : player.statusInfo(),
        player.fitness().stream().map(BiwengerMapper::form).toList(),
        player.points(),
        player.pointsHome(),
        player.pointsAway(),
        player.playedHome(),
        player.playedAway());
  }

  /** Every club, ordered by id. */
  public static List<Team> teams(Catalog catalog) {
    return catalog.teams().values().stream()
        .map(team -> new Team(team.id(), team.name(), team.slug()))
        .sorted(Comparator.comparingLong(Team::id))
        .toList();
  }

  /** The season's rounds, in Biwenger's order (the calendar, not the round number). */
  public static List<Round> rounds(Catalog catalog) {
    return catalog.season().rounds().stream()
        .map(
            round ->
                new Round(
                    round.id(),
                    round.name(),
                    round.shortName(),
                    roundStatus(round.status()),
                    round.part() == null ? 1 : round.part()))
        .toList();
  }

  public static League league(io.github.davidms.biwenger.client.model.League league) {
    return new League(
        league.id(),
        league.name(),
        league.scoreId(),
        league.users().stream()
            .map(BiwengerMapper::manager)
            .sorted(Comparator.comparingLong(Manager::id))
            .toList());
  }

  /** Today's market, in Biwenger's order. */
  public static List<MarketListing> listings(Market market) {
    return market.sales().stream().map(BiwengerMapper::listing).toList();
  }

  public static MarketListing listing(Sale sale) {
    Seller seller =
        sale.user() == null ? new Seller.Game() : new Seller.ByManager(manager(sale.user()));
    return new MarketListing(
        sale.player().id(),
        seller,
        Money.euros(sale.price()),
        Instant.ofEpochSecond(sale.date()),
        Instant.ofEpochSecond(sale.until()));
  }

  public static Manager manager(io.github.davidms.biwenger.client.model.Manager manager) {
    return new Manager(manager.id(), manager.name());
  }

  /** Biwenger's position codes: 1 goalkeeper, 2 defender, 3 midfielder, 4 forward, 5 coach. */
  static Position position(int code) {
    return switch (code) {
      case 1 -> Position.GOALKEEPER;
      case 2 -> Position.DEFENDER;
      case 3 -> Position.MIDFIELDER;
      case 4 -> Position.FORWARD;
      case 5 -> Position.COACH;
      // A position we don't know would put a player in the wrong lineup slot: fail loudly.
      default -> throw new IllegalArgumentException("Unknown Biwenger position code: " + code);
    };
  }

  /**
   * Biwenger's status strings. One we don't know yet becomes {@code UNKNOWN} rather than breaking
   * the whole catalog; the advisor treats it as "not available".
   */
  static PlayerStatus status(String status) {
    return switch (status) {
      case "ok" -> PlayerStatus.AVAILABLE;
      case "doubt" -> PlayerStatus.DOUBTFUL;
      case "injured" -> PlayerStatus.INJURED;
      case "sanctioned" -> PlayerStatus.SUSPENDED;
      default -> PlayerStatus.UNKNOWN;
    };
  }

  static RoundForm form(Fitness fitness) {
    if (fitness.played()) {
      return new RoundForm.Played(fitness.points());
    }
    if (fitness.status() == null) {
      return new RoundForm.NotPicked();
    }
    PlayerStatus reason = status(fitness.status());
    // "ok" in a missed round means fit but not picked.
    return reason.isAvailable() ? new RoundForm.NotPicked() : new RoundForm.Absent(reason);
  }

  /** Like player statuses, a round status we don't know yet must not break the whole catalog. */
  static RoundStatus roundStatus(String status) {
    return switch (status) {
      case "finished" -> RoundStatus.FINISHED;
      case "active" -> RoundStatus.ACTIVE;
      case "pending" -> RoundStatus.PENDING;
      default -> RoundStatus.UNKNOWN;
    };
  }
}
