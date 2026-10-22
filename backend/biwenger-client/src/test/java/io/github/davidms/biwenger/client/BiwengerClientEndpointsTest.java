package io.github.davidms.biwenger.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.client.model.Account;
import io.github.davidms.biwenger.client.model.BoardEntry;
import io.github.davidms.biwenger.client.model.BoardItem;
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.CatalogPlayer;
import io.github.davidms.biwenger.client.model.Fitness;
import io.github.davidms.biwenger.client.model.League;
import io.github.davidms.biwenger.client.model.Market;
import io.github.davidms.biwenger.client.model.MatchReport;
import io.github.davidms.biwenger.client.model.PlayerPrices;
import io.github.davidms.biwenger.client.model.PlayerReports;
import io.github.davidms.biwenger.client.model.PricePoint;
import io.github.davidms.biwenger.client.model.Round;
import io.github.davidms.biwenger.client.model.RoundStandings;
import io.github.davidms.biwenger.client.model.Sale;
import io.github.davidms.biwenger.client.model.Squad;
import io.github.davidms.biwenger.client.model.Standing;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Each read endpoint against its anonymised sample from docs/biwenger-api/samples/. The samples
 * hold many fields the records don't map, so every test also shows that unknown fields are ignored.
 */
class BiwengerClientEndpointsTest {

  // Plain-text HTTP/2 off: see BiwengerClientAuthTest.
  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  // The anonymised ids in the samples.
  private static final long LEAGUE_ID = 1_000_001;
  private static final long ME = 2_000_001;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final String REPORTS_URL =
      "/api/v2/players/la-liga/%s?lang=es&fields="
          + "*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away),rawStats)";

  private BiwengerClient client;

  @BeforeEach
  void setUp() throws IOException {
    biwenger.stubFor(post(BiwengerSession.LOGIN_PATH).willReturn(okJson("{\"token\": \"t\"}")));
    stub(BiwengerClient.ACCOUNT_PATH, "account.json");
    client =
        new BiwengerClient(
            RestClient.create(biwenger.baseUrl()),
            RestClient.create(biwenger.baseUrl()),
            new BiwengerCredentials("manager@example.com", "s3cret-password"));
  }

  @Test
  void account() {
    Account account = client.account();

    assertThat(account.account().id()).isEqualTo(3_000_001);
    assertThat(account.leagues())
        .singleElement()
        .satisfies(
            league -> {
              assertThat(league.competition()).isEqualTo("la-liga");
              assertThat(league.scoreId()).isEqualTo(1);
              assertThat(league.user().id()).isEqualTo(ME);
              assertThat(league.user().balance()).isEqualTo(51_420_000);
              assertThat(league.user().position()).isEqualTo(6);
              assertThat(league.settings().clause()).isEqualTo("steal");
              assertThat(league.settings().balance()).isEqualTo("hidden");
              assertThat(league.settings().lineupMaxClubPlayers()).isZero();
            });
  }

  @Test
  void league() throws IOException {
    stub("/api/v2/league/" + LEAGUE_ID, "league.json");

    League league = client.league();

    assertThat(league.id()).isEqualTo(LEAGUE_ID);
    assertThat(league.name()).isEqualTo("Example League");
    assertThat(league.users()).hasSize(6).extracting("id").contains(ME);
  }

  @Test
  void squad() throws IOException {
    stub("/api/v2/user?fields=players(id,owner)", "user-squad.json");

    Squad squad = client.squad();

    assertThat(squad.players()).hasSize(13);
    assertThat(squad.players().getFirst())
        .satisfies(
            player -> {
              assertThat(player.id()).isEqualTo(2001);
              assertThat(player.owner().clause()).isEqualTo(9_560_000);
              assertThat(player.owner().price()).isNull();
            });
  }

  @Test
  void market() throws IOException {
    stub("/api/v2/market", "market.json");

    Market market = client.market();

    assertThat(market.status().balance()).isEqualTo(51_420_000);
    assertThat(market.status().maximumBid()).isEqualTo(57_005_000);
    assertThat(market.sales()).hasSize(19);
    assertThat(market.sales()).filteredOn(Sale::isFreePlayer).hasSize(12);
    assertThat(sale(market, 15430))
        .satisfies(
            sale -> {
              assertThat(sale.user().id()).isEqualTo(2_000_003);
              assertThat(sale.price()).isEqualTo(6_930_000);
              assertThat(sale.until()).isEqualTo(1_792_793_067);
              assertThat(sale.player().owner().price()).isEqualTo(10_610_820);
              assertThat(sale.player().owner().clauseLockedUntil()).isNull();
            });
    assertThat(sale(market, 26879).player().owner().clauseLockedUntil()).isEqualTo(1_793_941_548L);
    assertThat(sale(market, 2169))
        .satisfies(
            sale -> {
              assertThat(sale.isFreePlayer()).isTrue();
              assertThat(sale.isExtended()).isTrue();
              assertThat(sale.player().owner()).isNull();
            });
    assertThat(sale(market, 16738).isExtended()).isFalse();
  }

  @Test
  void catalogUsesTheLeaguesScoringSystem() throws IOException {
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", "catalog.json");

    Catalog catalog = client.catalog();

    assertThat(catalog.scoreId()).isEqualTo(1);
    assertThat(catalog.players()).hasSize(39);
    assertThat(catalog.teams()).hasSize(20);
    assertThat(catalog.teams().get(19L).name()).isEqualTo("Villarreal");
    assertThat(catalog.season().id()).isEqualTo("2027");
    assertThat(catalog.season().rounds()).hasSize(42);
    assertThat(catalog.season().rounds())
        .filteredOn(round -> round.id() == 4937)
        .singleElement()
        .extracting(Round::shortName, Round::part, Round::status)
        .containsExactly("J1", 2, "finished");
  }

  @Test
  void catalogPlayer() throws IOException {
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", "catalog.json");

    CatalogPlayer ayoze = client.catalog().players().get(2001L);

    assertThat(ayoze.name()).isEqualTo("Ayoze");
    assertThat(ayoze.slug()).isEqualTo("ayoze-perez");
    assertThat(ayoze.teamId()).isEqualTo(19);
    assertThat(ayoze.position()).isEqualTo(4);
    assertThat(ayoze.price()).isEqualTo(4_780_000);
    assertThat(ayoze.priceIncrement()).isEqualTo(30_000);
    assertThat(ayoze.points()).isEqualTo(ayoze.pointsHome() + ayoze.pointsAway()).isEqualTo(35);
    assertThat(ayoze.pointsLastSeason()).isEqualTo(73);
    assertThat(ayoze.altPositions()).isEmpty();
    assertThat(ayoze.fitness()).extracting(Fitness::points).containsExactly(6, 2, 9, 6, 0);
    assertThat(ayoze.statusInfo()).isNull();
  }

  @Test
  void catalogOptionalPlayerFields() throws IOException {
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", "catalog.json");

    var players = client.catalog().players();

    assertThat(players.get(2602L).altPositions()).containsExactly(4);
    assertThat(players.get(17687L).pointsLastSeason()).isNull();
  }

  @Test
  void catalogIteratesPlayersInIdOrder() throws IOException {
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", "catalog.json");

    // The same order on every run, so anything built from it is deterministic (golden rule 5).
    assertThat(List.copyOf(client.catalog().players().keySet())).isSorted();
  }

  @Test
  void fitnessMixesPointsStatusesAndRoundsNotPlayed() throws IOException {
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", "catalog.json");

    // In the sample: ["sanctioned", -4, null, "doubt", "doubt"].
    List<Fitness> fitness = client.catalog().players().get(29034L).fitness();

    assertThat(fitness)
        .extracting(Fitness::points, Fitness::status)
        .containsExactly(
            tuple(null, "sanctioned"),
            tuple(-4, null),
            tuple(null, null),
            tuple(null, "doubt"),
            tuple(null, "doubt"));
    assertThat(fitness.get(2)).isEqualTo(Fitness.NOT_PLAYED);
    assertThat(fitness).filteredOn(Fitness::played).hasSize(1);
  }

  @Test
  void injuredPlayerHasStatusInfo() throws IOException {
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", "catalog.json");

    CatalogPlayer injured = client.catalog().players().get(37719L);

    assertThat(injured.status()).isEqualTo("injured");
    assertThat(injured.statusInfo()).startsWith("Rotura fibrilar");
    assertThat(injured.statusChange()).isEqualTo(1_792_609_209L);
  }

  @Test
  void roundStandings() throws IOException {
    stub("/api/v2/rounds/league", "rounds-league.json");

    RoundStandings rounds = client.roundStandings();

    assertThat(rounds.round().id()).isEqualTo(5176);
    assertThat(rounds.league().standings())
        .hasSize(6)
        .extracting(Standing::position)
        .containsExactly(1, 2, 3, 4, 5, 6);
    assertThat(rounds.league().standings().getFirst())
        .extracting(Standing::name, Standing::points)
        .containsExactly("Manager 4", 515);
    Standing me = standing(rounds, ME);
    assertThat(me.teamValue()).isEqualTo(22_340_000);
    assertThat(me.teamValueInc()).isEqualTo(-70_000);
    assertThat(me.lineup().type()).isEqualTo("3-4-3");
    assertThat(me.lineup().players()).hasSize(11).containsOnlyOnce((Long) null);
    assertThat(me.lineup().reserves()).hasSize(4);
    assertThat(me.lineup().captain()).isNull();
    assertThat(me.lineup().coach()).isNull();
    assertThat(me.lineup().points()).isEqualTo(20);
    Standing leader = standing(rounds, 2_000_004);
    assertThat(leader.lineup().captain().id()).isEqualTo(26930);
    assertThat(leader.lineup().coach().id()).isEqualTo(35316);
    assertThat(leader.lineup().points()).isEqualTo(71);
  }

  @Test
  void aMissingRequiredObjectIsReportedAsAFormatChange() throws IOException {
    ObjectNode rounds = (ObjectNode) JSON.readTree(sample("rounds-league.json"));
    ((ObjectNode) rounds.get("data").get("league").get("standings").get(0)).remove("lineup");
    biwenger.stubFor(
        get("/api/v2/rounds/league").willReturn(okJson(JSON.writeValueAsString(rounds))));

    assertThatThrownBy(() -> client.roundStandings())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage(
            "Biwenger's answer to GET /api/v2/rounds/league doesn't match docs/biwenger-api/");
  }

  @Test
  void board() throws IOException {
    stub("/api/v2/league/" + LEAGUE_ID + "/board?type=transfer,market", "board.json");

    List<BoardEntry> board = client.board();

    assertThat(board).hasSize(10);
    assertThat(board.getFirst().type()).isEqualTo("transfer");
    assertThat(item(board, 17937))
        .satisfies(
            clause -> {
              assertThat(clause.type()).isEqualTo("clause");
              assertThat(clause.from().id()).isEqualTo(ME);
              assertThat(clause.to().id()).isEqualTo(2_000_002);
              assertThat(clause.amount()).isEqualTo(8_960_000);
              assertThat(clause.bids()).isEmpty();
            });
    assertThat(item(board, 22964))
        .satisfies(
            auction -> {
              assertThat(auction.from()).isNull();
              assertThat(auction.to().id()).isEqualTo(2_000_003);
              assertThat(auction.bids())
                  .singleElement()
                  .satisfies(
                      bid -> {
                        assertThat(bid.user().id()).isEqualTo(2_000_002);
                        assertThat(bid.amount()).isEqualTo(3_145_000);
                      });
            });
  }

  @Test
  void playerPricesAreDatedValuesOldestFirst() throws IOException {
    stub("/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices", "player-prices.json");

    PlayerPrices prices = client.playerPrices("ayoze-perez");

    assertThat(prices.id()).isEqualTo(2001);
    assertThat(prices.prices()).hasSize(366);
    assertThat(prices.prices().getFirst())
        .isEqualTo(new PricePoint(LocalDate.of(2025, 10, 22), 6_730_000));
    assertThat(prices.prices().getLast())
        .isEqualTo(new PricePoint(LocalDate.of(2026, 10, 22), prices.price()));
    assertThat(prices.prices()).extracting(PricePoint::date).isSorted().doesNotHaveDuplicates();
  }

  @Test
  void playerReportsOfAPlayerWhoPlayedEveryMatch() throws IOException {
    stub(REPORTS_URL.formatted("ayoze-perez"), "player-reports.json");

    PlayerReports ayoze = client.playerReports("ayoze-perez");

    assertThat(ayoze.reports()).hasSize(9).allMatch(MatchReport::played);
    assertThat(ayoze.reports())
        .extracting(MatchReport::minutesPlayed)
        .containsExactly(56, 61, 28, 62, 14, 63, 67, 90, 84);
    // Summed in the league's scoring system (1), the reports give the catalog's 35 points.
    assertThat(ayoze.reports().stream().mapToInt(report -> report.points(1).orElseThrow()).sum())
        .isEqualTo(35);
    MatchReport first = ayoze.reports().getFirst();
    assertThat(first.home()).isFalse();
    assertThat(first.match().round().shortName()).isEqualTo("J1");
    assertThat(first.points()).containsKeys(1, 2, 3, 5, 6, 7, 8);
    assertThat(ayoze.reports().get(5).match().round().part()).isEqualTo(2);
  }

  @Test
  void playerReportsSayWhyAPlayerMissedAMatch() throws IOException {
    stub(REPORTS_URL.formatted("pablo-marin"), "player-reports-missed-matches.json");

    List<MatchReport> reports = client.playerReports("pablo-marin").reports();

    assertThat(reports)
        .extracting(r -> r.match().round().shortName(), r -> statusOf(r), MatchReport::played)
        .containsExactly(
            tuple("J2", "injured", false),
            tuple("J1", "injured", false),
            tuple("J3", "doubt", false),
            tuple("J6", "injured", false),
            tuple("J4", "doubt", false),
            tuple("J5", "doubt", false),
            tuple("J7", "ok", false),
            tuple("J8", null, true),
            tuple("J9", "sanctioned", false));
    MatchReport played = reports.get(7);
    assertThat(played.points(1)).hasValue(-4);
    assertThat(played.minutesPlayed()).isEqualTo(66);
    MatchReport injured = reports.getFirst();
    assertThat(injured.points()).isEmpty();
    assertThat(injured.points(1)).isEmpty();
    assertThat(injured.minutesPlayed()).isZero();
  }

  @Test
  void cdnCallsNeitherLogInNorSendTheToken() throws IOException {
    stub("/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices", "player-prices.json");

    client.playerPrices("ayoze-perez");

    biwenger.verify(0, postRequestedFor(urlEqualTo(BiwengerSession.LOGIN_PATH)));
    biwenger.verify(
        getRequestedFor(urlEqualTo("/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices"))
            .withoutHeader("Authorization")
            .withoutHeader("X-League")
            .withHeader("X-Lang", equalTo("es")));
  }

  @Test
  void aPriceWithAnImpossibleDateIsReportedAsAFormatChange() {
    biwenger.stubFor(
        get(urlEqualTo("/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices"))
            .willReturn(
                okJson(
                    """
                    {"status": 200, "data": {"id": 2001, "name": "Ayoze", "slug": "ayoze-perez",
                      "position": 4, "price": 1, "priceIncrement": 0, "status": "ok",
                      "prices": [[251322, 1]]}}
                    """)));

    assertThatThrownBy(() -> client.playerPrices("ayoze-perez"))
        .isInstanceOf(BiwengerApiException.class)
        .hasMessageContaining("doesn't match docs/biwenger-api/");
  }

  @Test
  void aSlugIsRequired() {
    assertThatThrownBy(() -> client.playerReports(" "))
        .isInstanceOf(IllegalArgumentException.class);
    biwenger.verify(
        0, getRequestedFor(urlEqualTo("/api/v2/players/la-liga/?lang=es&fields=*,prices")));
  }

  private static @Nullable String statusOf(MatchReport report) {
    return report.status() == null ? null : report.status().status();
  }

  @Test
  void ignoresUnknownFieldsEvenIfTheCallersMapperRejectsThem() throws IOException {
    // A caller (Boot's spring.jackson settings, say) might configure a strict mapper.
    var strict =
        JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    var strictRestClient =
        RestClient.builder()
            .baseUrl(biwenger.baseUrl())
            .configureMessageConverters(
                converters ->
                    converters.withJsonConverter(new JacksonJsonHttpMessageConverter(strict)))
            .build();
    stub("/api/v2/market", "market.json");

    Market market =
        new BiwengerClient(
                strictRestClient, strictRestClient, new BiwengerCredentials("m@example.com", "pw"))
            .market();

    assertThat(market.sales()).hasSize(19);
  }

  @Test
  void aMissingRequiredFieldIsReportedAsAFormatChange() {
    biwenger.stubFor(
        get("/api/v2/market")
            .willReturn(okJson("{\"status\": 200, \"data\": {\"status\": {\"balance\": 1}}}")));

    assertThatThrownBy(() -> client.market())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage("Biwenger's answer to GET /api/v2/market doesn't match docs/biwenger-api/");
  }

  @Test
  void sendsTheLeagueHeadersOnEndpointCalls() throws IOException {
    stub("/api/v2/rounds/league", "rounds-league.json");

    client.roundStandings();

    biwenger.verify(
        getRequestedFor(urlEqualTo("/api/v2/rounds/league"))
            .withHeader("X-League", equalTo(String.valueOf(LEAGUE_ID)))
            .withHeader("X-User", equalTo(String.valueOf(ME))));
  }

  private static Sale sale(Market market, long playerId) {
    return market.sales().stream()
        .filter(s -> s.player().id() == playerId)
        .findFirst()
        .orElseThrow();
  }

  private static Standing standing(RoundStandings rounds, long managerId) {
    return rounds.league().standings().stream()
        .filter(s -> s.id() == managerId)
        .findFirst()
        .orElseThrow();
  }

  private static BoardItem item(List<BoardEntry> board, long playerId) {
    return board.stream()
        .flatMap(entry -> entry.content().stream())
        .filter(i -> i.player() == playerId)
        .findFirst()
        .orElseThrow();
  }

  private static void stub(String url, String sample) throws IOException {
    biwenger.stubFor(get(urlEqualTo(url)).willReturn(okJson(sample(sample))));
  }

  private static String sample(String name) throws IOException {
    try (InputStream in =
        BiwengerClientEndpointsTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
