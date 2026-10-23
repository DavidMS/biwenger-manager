package io.github.davidms.biwenger.app.backfill;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.backfill.service.BackfillService;
import io.github.davidms.biwenger.app.backfill.service.BackfillSummary;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository;
import io.github.davidms.biwenger.client.model.Catalog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The backfill end to end: WireMock answers every player's CDN calls with Ayoze's samples (366
 * prices, 9 matches), PostgreSQL stores them.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BackfillTest.FixedClock.class})
class BackfillTest {

  // Plain-text HTTP/2 off: see BiwengerClientAuthTest in biwenger-client.
  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  static final Instant NOW = Instant.parse("2026-10-22T05:30:00Z");
  static final int PLAYERS = 39;
  static final String CDN = "/cdn/api/v2/players/la-liga/";

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClock {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(NOW, ZoneId.of("Europe/Madrid"));
    }
  }

  @DynamicPropertySource
  static void biwengerSettings(DynamicPropertyRegistry registry) {
    registry.add("biwenger.base-url", biwenger::baseUrl);
    registry.add("biwenger.cdn-base-url", () -> biwenger.baseUrl() + "/cdn");
    registry.add("biwenger.email", () -> "manager@example.com");
    registry.add("biwenger.password", () -> "s3cret-password");
    registry.add("biwenger.min-request-interval", () -> "0s");
    registry.add("biwenger.max-attempts", () -> "1");
  }

  @Autowired BackfillService backfill;
  @Autowired PlayerPriceSnapshotRepository prices;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void freshDatabaseAndBiwenger() throws IOException {
    emptyTables();
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(
        get(urlEqualTo("/api/v2/competitions/la-liga/data?lang=es&score=1"))
            .willReturn(okJson(sample("catalog.json"))));
    biwenger.stubFor(
        get(urlPathMatching(CDN + ".*"))
            .withQueryParam("fields", equalTo("*,prices"))
            .willReturn(okJson(sample("player-prices.json"))));
    biwenger.stubFor(
        get(urlPathMatching(CDN + ".*"))
            .withQueryParam("fields", matching("\\*,reports\\(.*"))
            .willReturn(okJson(sample("player-reports.json"))));
  }

  /** The container is shared with other test classes, which count rows: leave nothing behind. */
  @AfterEach
  void emptyTables() {
    jdbc.execute("TRUNCATE player_price_snapshot, player_points_report, backfill_progress, player");
  }

  @Test
  void importsEveryPlayersPricesAndMatches() {
    BackfillSummary summary = backfill.run();

    assertThat(summary.season()).isEqualTo("2027");
    assertThat(summary.completed()).isEqualTo(PLAYERS);
    assertThat(summary.isFinished()).isTrue();
    // 366 days of history, but today's belongs to the daily sync.
    assertThat(count("player_price_snapshot")).isEqualTo(PLAYERS * 365L);
    assertThat(count("player_points_report")).isEqualTo(PLAYERS * 9L);
    assertThat(count("backfill_progress")).isEqualTo(PLAYERS);
    // Two CDN calls per player: prices and reports.
    assertThat(cdnCalls()).isEqualTo(2 * PLAYERS);
  }

  @Test
  void aFinishedBackfillDoesNothing() {
    backfill.run();
    int callsBefore = cdnCalls();

    BackfillSummary again = backfill.run();

    assertThat(again.completed()).isZero();
    assertThat(again.alreadyDone()).isEqualTo(PLAYERS);
    assertThat(cdnCalls()).isEqualTo(callsBefore);
  }

  @Test
  void resumesWithThePlayersThatFailed() {
    String slug = firstPlayerSlug();
    biwenger.stubFor(
        get(urlPathEqualTo(CDN + slug))
            .withQueryParam("fields", equalTo("*,prices"))
            .willReturn(aResponse().withStatus(503)));

    BackfillSummary first = backfill.run();

    assertThat(first.failed()).isEqualTo(1);
    assertThat(first.remaining()).isEqualTo(1);
    assertThat(count("backfill_progress")).isEqualTo(PLAYERS - 1);

    // Biwenger answers again: the next run only does the player who failed.
    biwenger.removeStub(
        get(urlPathEqualTo(CDN + slug)).withQueryParam("fields", equalTo("*,prices")));
    biwenger.resetRequests();

    BackfillSummary second = backfill.run();

    assertThat(second.completed()).isEqualTo(1);
    assertThat(second.isFinished()).isTrue();
    assertThat(cdnCalls()).isEqualTo(2);
    biwenger.verify(2, getRequestedFor(urlPathEqualTo(CDN + slug)));
  }

  @Test
  void leavesTodayToTheDailySync() {
    backfill.run();

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM player_price_snapshot WHERE snapshot_date = DATE '2026-10-22'",
                Long.class))
        .isZero();
  }

  @Test
  void storesMissedMatchesWithTheirReason() throws IOException {
    String slug = firstPlayerSlug();
    long playerId = firstPlayerId();
    biwenger.stubFor(
        get(urlPathEqualTo(CDN + slug))
            .withQueryParam("fields", matching("\\*,reports\\(.*"))
            .willReturn(okJson(sample("player-reports-missed-matches.json"))));

    backfill.run();

    var rows =
        jdbc.queryForList(
            "SELECT played, points, minutes_played, absence FROM player_points_report"
                + " WHERE player_id = ? ORDER BY match_date",
            playerId);
    assertThat(rows).hasSize(9);
    assertThat(rows.getFirst())
        .containsEntry("played", false)
        .containsEntry("points", null)
        .containsEntry("minutes_played", 0)
        .containsEntry("absence", "INJURED");
    assertThat(rows.get(6)).containsEntry("absence", "AVAILABLE"); // fit but not picked
    assertThat(rows.get(7)).containsEntry("played", true).containsEntry("points", -4);
  }

  @Test
  void aStopRequestOnlyAppliesToARunInProgress() {
    backfill.requestStop();

    assertThat(backfill.run().completed()).isEqualTo(PLAYERS);
  }

  @Test
  void neverOverwritesADayTheDailySyncStored() {
    // The daily sync's row for Ayoze on the last day of his price history.
    var ayoze =
        BiwengerMapper.players(Samples.data("catalog.json", Catalog.class)).stream()
            .filter(p -> p.id() == 2001)
            .findFirst()
            .orElseThrow();
    prices.save(PlayerPriceSnapshotEntity.daily(ayoze, LocalDate.of(2026, 10, 22), NOW));

    backfill.run();

    var day =
        jdbc.queryForMap(
            "SELECT source, points FROM player_price_snapshot WHERE player_id = 2001"
                + " AND snapshot_date = DATE '2026-10-22'");
    assertThat(day).containsEntry("source", "DAILY_SYNC").containsEntry("points", 35);
  }

  @Test
  void stopsAfterThreeFailuresInARow() {
    biwenger.stubFor(
        get(urlPathMatching(CDN + ".*"))
            .withQueryParam("fields", equalTo("*,prices"))
            .willReturn(aResponse().withStatus(503)));

    BackfillSummary summary = backfill.run();

    assertThat(summary.failed()).isEqualTo(3);
    assertThat(summary.completed()).isZero();
    assertThat(summary.remaining()).isEqualTo(PLAYERS);
    // It stopped asking: three players' price calls, nothing more.
    assertThat(cdnCalls()).isEqualTo(3);
  }

  @Test
  void storesPointsInTheLeaguesScoringSystem() {
    backfill.run();

    // Ayoze's points in the league's system (1) add up to the catalog's 35.
    assertThat(
            jdbc.queryForObject(
                "SELECT sum(points) FROM player_points_report WHERE player_id = 2001 AND score_id = 1",
                Long.class))
        .isEqualTo(35);
  }

  private int cdnCalls() {
    return biwenger.findAll(getRequestedFor(urlPathMatching(CDN + ".*"))).size();
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private static String firstPlayerSlug() {
    return BiwengerMapper.players(Samples.data("catalog.json", Catalog.class)).getFirst().slug();
  }

  private static long firstPlayerId() {
    return BiwengerMapper.players(Samples.data("catalog.json", Catalog.class)).getFirst().id();
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BackfillTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
