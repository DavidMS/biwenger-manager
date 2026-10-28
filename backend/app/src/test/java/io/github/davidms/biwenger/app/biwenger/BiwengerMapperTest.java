package io.github.davidms.biwenger.app.biwenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.dashboard.domain.TeamStatus;
import io.github.davidms.biwenger.app.league.domain.League;
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
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.Fitness;
import io.github.davidms.biwenger.client.model.Market;
import io.github.davidms.biwenger.client.model.MatchReport;
import io.github.davidms.biwenger.client.model.PlayerPrices;
import io.github.davidms.biwenger.client.model.PlayerReports;
import io.github.davidms.biwenger.client.model.RoundStandings;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The mapper against the anonymised samples from docs/biwenger-api/samples/. No Spring, no HTTP.
 */
class BiwengerMapperTest {

  // The client's own setting (BiwengerClient), stated here too so the two can't drift apart.
  private static final JsonMapper JSON =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static Catalog catalog;
  private static Market market;
  private static io.github.davidms.biwenger.client.model.League league;

  @BeforeAll
  static void readSamples() throws IOException {
    catalog = data("catalog.json", Catalog.class);
    market = data("market.json", Market.class);
    league = data("league.json", io.github.davidms.biwenger.client.model.League.class);
  }

  @Test
  void mapsEveryCatalogPlayerInIdOrder() {
    List<Player> players = BiwengerMapper.players(catalog);

    assertThat(players).hasSize(39);
    assertThat(players).extracting(Player::id).isSorted();
  }

  @Test
  void mapsAPlayer() {
    Player ayoze = player(2001);

    assertThat(ayoze.name()).isEqualTo("Ayoze");
    assertThat(ayoze.position()).isEqualTo(Position.FORWARD);
    assertThat(ayoze.price()).isEqualTo(Money.euros(4_780_000));
    assertThat(ayoze.priceChange()).isEqualTo(Money.euros(30_000));
    assertThat(ayoze.status()).isEqualTo(PlayerStatus.AVAILABLE);
    assertThat(ayoze.statusNote()).isEmpty();
    assertThat(ayoze.points()).isEqualTo(35);
    assertThat(ayoze.gamesPlayed()).isEqualTo(9);
    assertThat(ayoze.form())
        .containsExactly(
            new RoundForm.Played(6),
            new RoundForm.Played(2),
            new RoundForm.Played(9),
            new RoundForm.Played(6),
            new RoundForm.Played(0));
  }

  @Test
  void mapsFormWithAbsencesAndRoundsNotPicked() {
    // In the sample: ["sanctioned", -4, null, "doubt", "doubt"].
    assertThat(player(29034).form())
        .containsExactly(
            new RoundForm.Absent(PlayerStatus.SUSPENDED),
            new RoundForm.Played(-4),
            new RoundForm.NotPicked(),
            new RoundForm.Absent(PlayerStatus.DOUBTFUL),
            new RoundForm.Absent(PlayerStatus.DOUBTFUL));
  }

  @Test
  void mapsAnInjuryWithItsNote() {
    Player injured = player(37719);

    assertThat(injured.status()).isEqualTo(PlayerStatus.INJURED);
    assertThat(injured.statusNote()).startsWith("Rotura fibrilar");
  }

  @Test
  void mapsOtherPositions() {
    // A defender allowed in midfield, as in the lineup saved in #19.
    Player bretones = player(33971);

    assertThat(bretones.position()).isEqualTo(Position.DEFENDER);
    assertThat(bretones.canPlay(Position.MIDFIELDER)).isTrue();
  }

  @Test
  void mapsTeamsAndRounds() {
    assertThat(BiwengerMapper.teams(catalog))
        .hasSize(20)
        .contains(new Team(19, "Villarreal", "villarreal"));
    List<Round> rounds = BiwengerMapper.rounds(catalog);
    assertThat(rounds).hasSize(42);
    assertThat(rounds)
        .filteredOn(round -> round.id() == 4937)
        .singleElement()
        .satisfies(
            postponed -> {
              assertThat(postponed.shortName()).isEqualTo("J1");
              assertThat(postponed.part()).isEqualTo(2);
              assertThat(postponed.isPostponed()).isTrue();
              assertThat(postponed.status()).isEqualTo(RoundStatus.FINISHED);
            });
    assertThat(rounds).filteredOn(round -> !round.isPostponed()).allMatch(r -> r.part() == 1);
  }

  @Test
  void mapsTheMarket() {
    List<MarketListing> listings = BiwengerMapper.listings(market);

    assertThat(listings).hasSize(19);
    assertThat(listings).filteredOn(MarketListing::isFreePlayer).hasSize(12);
    MarketListing byManager =
        listings.stream().filter(l -> l.playerId() == 15430).findFirst().orElseThrow();
    assertThat(byManager.seller())
        .isEqualTo(
            new Seller.ByManager(
                new Manager(2_000_003, "Manager 3"),
                new Ownership(
                    Money.euros(21_221_640),
                    Money.euros(10_610_820),
                    Instant.ofEpochSecond(1_786_856_548),
                    Instant.ofEpochSecond(1_786_856_548))));
    assertThat(byManager.price()).isEqualTo(Money.euros(6_930_000));
    assertThat(byManager.closesAt()).isEqualTo(Instant.ofEpochSecond(1_792_793_067));
  }

  @Test
  void mapsAClauseLockOnASale() throws IOException {
    MarketListing locked =
        BiwengerMapper.listings(market).stream()
            .filter(l -> l.playerId() == 26879)
            .findFirst()
            .orElseThrow();

    var ownership = ((Seller.ByManager) locked.seller()).ownership();
    assertThat(ownership.clauseUnlocksAt()).isEqualTo(Instant.ofEpochSecond(1_793_941_548));
    assertThat(ownership.clauseLockedAt(Instant.ofEpochSecond(1_792_745_310))).isTrue();
  }

  @Test
  void aManagersSaleWithoutOwnershipDataIsAFormatChange() {
    var sale =
        new io.github.davidms.biwenger.client.model.Sale(
            new io.github.davidms.biwenger.client.model.SalePlayer(15430, null),
            6_930_000,
            1_792_620_267,
            1_792_793_067,
            null,
            new io.github.davidms.biwenger.client.model.Manager(2_000_003, "Manager 3", null));

    assertThatThrownBy(() -> BiwengerMapper.listing(sale))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Biwenger's market doesn't match docs/biwenger-api/");
  }

  @Test
  void mapsTheStandingsByPosition() throws IOException {
    List<Standing> standings =
        BiwengerMapper.standings(data("rounds-league.json", RoundStandings.class));

    assertThat(standings)
        .hasSize(6)
        .extracting(Standing::position)
        .containsExactly(1, 2, 3, 4, 5, 6);
    assertThat(standings.getFirst().manager().name()).isEqualTo("Manager 4");
    Standing me =
        standings.stream().filter(s -> s.manager().id() == 2_000_001).findFirst().orElseThrow();
    assertThat(me.teamValue()).isEqualTo(Money.euros(22_340_000));
    assertThat(me.teamValueChange()).isEqualTo(Money.euros(-70_000));
  }

  @Test
  void mapsAPriceHistory() {
    List<DailyPrice> prices =
        BiwengerMapper.prices(sampleData("player-prices.json", PlayerPrices.class));

    assertThat(prices).hasSize(366);
    assertThat(prices.getFirst())
        .isEqualTo(new DailyPrice(LocalDate.of(2025, 10, 22), Money.euros(6_730_000)));
  }

  @Test
  void mapsMatchesPlayedAndMissed() {
    List<MatchResult> ayoze =
        BiwengerMapper.matches(sampleData("player-reports.json", PlayerReports.class), 1);
    assertThat(ayoze).hasSize(9).allMatch(m -> m.outcome() instanceof MatchResult.Played);
    assertThat(ayoze.getFirst().outcome()).isEqualTo(new MatchResult.Played(2, 56));

    List<MatchResult> marin =
        BiwengerMapper.matches(
            sampleData("player-reports-missed-matches.json", PlayerReports.class), 1);
    assertThat(marin)
        .extracting(MatchResult::outcome)
        .containsExactly(
            new MatchResult.Missed(PlayerStatus.INJURED),
            new MatchResult.Missed(PlayerStatus.INJURED),
            new MatchResult.Missed(PlayerStatus.DOUBTFUL),
            new MatchResult.Missed(PlayerStatus.INJURED),
            new MatchResult.Missed(PlayerStatus.DOUBTFUL),
            new MatchResult.Missed(PlayerStatus.DOUBTFUL),
            new MatchResult.Missed(PlayerStatus.AVAILABLE),
            new MatchResult.Played(-4, 66),
            new MatchResult.Missed(PlayerStatus.SUSPENDED));
  }

  @Test
  void aMissedMatchWithoutAStatusIsUnknown() {
    var first =
        sampleData("player-reports-missed-matches.json", PlayerReports.class).reports().getFirst();
    var noStatus = new MatchReport(first.match(), first.home(), Map.of(), null, null);

    assertThat(BiwengerMapper.match(noStatus, 1).outcome())
        .isEqualTo(new MatchResult.Missed(PlayerStatus.UNKNOWN));
  }

  @Test
  void mapsTheConnectedAccount() {
    var account = sampleData("account.json", Account.class);

    // In the sample the account's name is the same as its team's (both anonymised).
    assertThat(BiwengerMapper.connectedAccount(account, 1_000_001))
        .isEqualTo(new ConnectedAccount("Manager 1", "Manager 1", "Example League"));
    assertThatThrownBy(() -> BiwengerMapper.connectedAccount(account, 42))
        .hasMessageContaining("doesn't match docs/biwenger-api/");
  }

  @Test
  void mapsYourTeamStatus() throws IOException {
    var account = sampleData("account.json", Account.class);
    var standings = data("rounds-league.json", RoundStandings.class);

    assertThat(BiwengerMapper.teamStatus(account, standings, 1_000_001, 2_000_001))
        .isEqualTo(
            new TeamStatus(
                Money.euros(51_420_000), 220, 6, 6, Money.euros(22_340_000), Money.euros(-70_000)));
    assertThatThrownBy(() -> BiwengerMapper.teamStatus(account, standings, 1_000_001, 42))
        .hasMessageContaining("standings don't match docs/biwenger-api/");
  }

  @Test
  void mapsTheScheduledRounds() {
    // The active round (J6, part 3) is under way and has no start: only J10 is scheduled.
    assertThat(BiwengerMapper.scheduledRounds(catalog))
        .containsExactly(
            new ScheduledRound(4908, "Jornada 10", Instant.ofEpochSecond(1_792_782_000)));
  }

  @Test
  void mapsTheLeagueWithManagersInIdOrder() {
    League mapped = BiwengerMapper.league(league);

    assertThat(mapped.name()).isEqualTo("Example League");
    assertThat(mapped.managers()).hasSize(6).extracting(Manager::id).isSorted();
  }

  @Test
  void translatesBiwengerCodes() {
    assertThat(BiwengerMapper.position(5)).isEqualTo(Position.COACH);
    assertThatThrownBy(() -> BiwengerMapper.position(6))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("6");
    // A status Biwenger adds later must not break the catalog.
    assertThat(BiwengerMapper.status("warned")).isEqualTo(PlayerStatus.UNKNOWN);
    assertThat(BiwengerMapper.roundStatus("cancelled")).isEqualTo(RoundStatus.UNKNOWN);
  }

  @Test
  void aMissedRoundWhileFitIsNotPicked() {
    assertThat(BiwengerMapper.form(new Fitness(null, "ok"))).isEqualTo(new RoundForm.NotPicked());
    assertThat(BiwengerMapper.form(Fitness.NOT_PLAYED)).isEqualTo(new RoundForm.NotPicked());
    assertThat(BiwengerMapper.form(new Fitness(null, "injured")))
        .isEqualTo(new RoundForm.Absent(PlayerStatus.INJURED));
  }

  @Test
  void domainInvariantsHold() {
    assertThatThrownBy(() -> new Round(1, "Jornada 1", "J1", RoundStatus.PENDING, 0))
        .isInstanceOf(IllegalArgumentException.class);
    Instant now = Instant.ofEpochSecond(1_792_745_310);
    assertThatThrownBy(
            () -> new MarketListing(1, new Seller.Game(), Money.ZERO, now, now.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Player player(long id) {
    return BiwengerMapper.players(catalog).stream()
        .filter(p -> p.id() == id)
        .findFirst()
        .orElseThrow();
  }

  private static <T> T sampleData(String sample, Class<T> type) {
    return Samples.data(sample, type);
  }

  private static <T> T data(String sample, Class<T> type) throws IOException {
    try (InputStream in = BiwengerMapperTest.class.getResourceAsStream("/samples/" + sample)) {
      assertThat(in).as("sample %s on the test classpath", sample).isNotNull();
      return JSON.treeToValue(JSON.readTree(in).get("data"), type);
    }
  }
}
