package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.dashboard.domain.TeamStatus;
import io.github.davidms.biwenger.app.league.domain.League;
import io.github.davidms.biwenger.app.league.domain.LeagueEntry;
import io.github.davidms.biwenger.app.league.domain.LeagueTable;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.league.domain.Round;
import io.github.davidms.biwenger.app.league.domain.RoundStatus;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import io.github.davidms.biwenger.app.league.domain.Standing;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.Ownership;
import io.github.davidms.biwenger.app.market.domain.Seller;
import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.RoundForm;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.client.model.Account;
import io.github.davidms.biwenger.client.model.AccountLeague;
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.CatalogPlayer;
import io.github.davidms.biwenger.client.model.Fitness;
import io.github.davidms.biwenger.client.model.Market;
import io.github.davidms.biwenger.client.model.MatchReport;
import io.github.davidms.biwenger.client.model.PlayerPrices;
import io.github.davidms.biwenger.client.model.PlayerRef;
import io.github.davidms.biwenger.client.model.PlayerReports;
import io.github.davidms.biwenger.client.model.RoundStandings;
import io.github.davidms.biwenger.client.model.Sale;
import io.github.davidms.biwenger.client.model.Squad;
import io.github.davidms.biwenger.client.model.SquadPlayer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  /** The account's name, its team in league {@code leagueId}, and that league's name. */
  public static ConnectedAccount connectedAccount(Account account, long leagueId) {
    var league = accountLeague(account, leagueId);
    return new ConnectedAccount(account.account().name(), league.user().name(), league.name());
  }

  private static AccountLeague accountLeague(Account account, long leagueId) {
    return account.leagues().stream()
        .filter(candidate -> candidate.id() == leagueId)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Biwenger's account doesn't match docs/biwenger-api/: league "
                        + leagueId
                        + " is missing"));
  }

  /**
   * The league table by position, each manager with their squad as their lineup shows it: starters
   * and reserves (empty slots skipped), the players left out and the coach.
   */
  public static LeagueTable leagueTable(RoundStandings rounds, long userId) {
    return new LeagueTable(
        rounds.league().standings().stream()
            .map(
                standing -> {
                  var lineup = standing.lineup();
                  var ids =
                      Stream.of(
                              lineup.players().stream(),
                              lineup.reserves().stream(),
                              lineup.discarded().stream(),
                              Stream.ofNullable(lineup.coach()).map(PlayerRef::id))
                          .flatMap(s -> s)
                          .filter(Objects::nonNull)
                          .toList();
                  return new LeagueEntry(
                      new Standing(
                          new Manager(standing.id(), standing.name()),
                          standing.position(),
                          standing.points(),
                          Money.euros(standing.teamValue()),
                          Money.euros(standing.teamValueInc())),
                      ids,
                      Instant.ofEpochSecond(lineup.date()));
                })
            .sorted(
                Comparator.comparingInt((LeagueEntry entry) -> entry.standing().position())
                    .thenComparingLong(LeagueEntry::managerId))
            .toList(),
        userId);
  }

  /**
   * Your team in league {@code leagueId}, where you are user {@code userId}: the balance from the
   * account (only it has it), everything else from one standings response, so position, points and
   * league size always agree.
   */
  public static TeamStatus teamStatus(
      Account account, RoundStandings standings, long leagueId, long userId) {
    var balance = accountLeague(account, leagueId).user().balance();
    var all = standings.league().standings();
    var mine =
        all.stream()
            .filter(standing -> standing.id() == userId)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Biwenger's standings don't match docs/biwenger-api/: user "
                            + userId
                            + " is missing"));
    return new TeamStatus(
        Money.euros(balance),
        mine.points(),
        mine.position(),
        all.size(),
        Money.euros(mine.teamValue()),
        Money.euros(mine.teamValueInc()));
  }

  /** The catalog's active and next rounds that have a start; one under way has none. */
  public static List<ScheduledRound> scheduledRounds(Catalog catalog) {
    return catalog.activeEvents().stream()
        .filter(event -> event.start() != null)
        .map(
            event ->
                new ScheduledRound(event.id(), event.name(), Instant.ofEpochSecond(event.start())))
        .toList();
  }

  /**
   * Every catalog player with a club, ordered by id. A player with no club (released during the
   * season) can't play in La Liga, so the app leaves them out; {@link #playersWithNoClub} lists
   * them.
   */
  public static List<Player> players(Catalog catalog) {
    return catalog.players().values().stream()
        .filter(player -> player.teamId() != null)
        .map(player -> player(player, player.teamId()))
        .sorted(Comparator.comparingLong(Player::id))
        .toList();
  }

  /** The ids of the catalog players with no club, ordered: the ones {@link #players} leaves out. */
  public static List<Long> playersWithNoClub(Catalog catalog) {
    return catalog.players().values().stream()
        .filter(player -> player.teamId() == null)
        .map(CatalogPlayer::id)
        .sorted()
        .toList();
  }

  private static Player player(CatalogPlayer player, long teamId) {
    return new Player(
        player.id(),
        player.name(),
        player.slug(),
        teamId,
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
        sale.user() == null
            ? new Seller.Game()
            : new Seller.ByManager(manager(sale.user()), ownership(sale));
    return new MarketListing(
        sale.player().id(),
        seller,
        Money.euros(sale.price()),
        Instant.ofEpochSecond(sale.date()),
        Instant.ofEpochSecond(sale.until()));
  }

  /** A manager's sale always carries how he owns the player, in the market's answer. */
  static Ownership ownership(Sale sale) {
    var owner = sale.player().owner();
    if (owner == null || owner.price() == null) {
      // Biwenger dropped data we rely on: report it as a format change, like the client does.
      throw new IllegalArgumentException(
          "Biwenger's market doesn't match docs/biwenger-api/: player "
              + sale.player().id()
              + " is sold by a manager but has no owner price");
    }
    Instant acquiredAt = Instant.ofEpochSecond(owner.date());
    return new Ownership(
        Money.euros(owner.clause()),
        Money.euros(owner.price()),
        acquiredAt,
        owner.clauseLockedUntil() == null
            ? acquiredAt
            : Instant.ofEpochSecond(owner.clauseLockedUntil()));
  }

  /** The ids of your players, in order. Some may be missing from the catalog. */
  public static List<Long> squadPlayerIds(Squad squad) {
    return squad.players().stream().map(SquadPlayer::id).sorted().toList();
  }

  /** A player's daily values, oldest first. */
  public static List<DailyPrice> prices(PlayerPrices prices) {
    return prices.prices().stream()
        .map(point -> new DailyPrice(point.date(), Money.euros(point.price())))
        .toList();
  }

  /**
   * A player's matches in date order, with points in {@code scoreId} when he played and the reason
   * when he didn't.
   */
  public static List<MatchResult> matches(PlayerReports reports, int scoreId) {
    return reports.reports().stream().map(report -> match(report, scoreId)).toList();
  }

  static MatchResult match(MatchReport report, int scoreId) {
    MatchResult.Outcome outcome;
    if (report.played()) {
      int points =
          report
              .points(scoreId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Biwenger's reports don't match docs/biwenger-api/: match "
                              + report.match().id()
                              + " has no points in scoring system "
                              + scoreId));
      outcome = new MatchResult.Played(points, report.minutesPlayed());
    } else {
      outcome =
          new MatchResult.Missed(
              report.status() == null ? PlayerStatus.UNKNOWN : status(report.status().status()));
    }
    return new MatchResult(
        report.match().id(),
        report.match().round().id(),
        Instant.ofEpochSecond(report.match().date()),
        report.home(),
        outcome);
  }

  /** The league table, by position. */
  public static List<Standing> standings(RoundStandings rounds) {
    return rounds.league().standings().stream()
        .map(
            standing ->
                new Standing(
                    new Manager(standing.id(), standing.name()),
                    standing.position(),
                    standing.points(),
                    Money.euros(standing.teamValue()),
                    Money.euros(standing.teamValueInc())))
        .sorted(Comparator.comparingInt(Standing::position))
        .toList();
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
