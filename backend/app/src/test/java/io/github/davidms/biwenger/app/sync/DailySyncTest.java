package io.github.davidms.biwenger.app.sync;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.sync.domain.SyncResult;
import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.service.DailySyncService;
import io.github.davidms.biwenger.app.sync.service.SyncAlreadyRunningException;
import io.github.davidms.biwenger.app.sync.service.SyncSummary;
import io.github.davidms.biwenger.client.BiwengerApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** The whole sync: WireMock serves the anonymised samples, PostgreSQL stores the snapshots. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, DailySyncTest.FixedClock.class})
@ExtendWith(OutputCaptureExtension.class)
class DailySyncTest {

  // Plain-text HTTP/2 off: see BiwengerClientAuthTest in biwenger-client.
  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  /** 07:30 in Madrid on 22 October 2026, when the scheduled sync runs. */
  static final Instant MORNING = Instant.parse("2026-10-22T05:30:00Z");

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClock {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(MORNING, ZoneId.of("Europe/Madrid"));
    }
  }

  @DynamicPropertySource
  static void biwengerSettings(DynamicPropertyRegistry registry) {
    registry.add("biwenger.base-url", biwenger::baseUrl);
    registry.add("biwenger.cdn-base-url", () -> biwenger.baseUrl() + "/cdn");
    registry.add("biwenger.email", () -> "manager@example.com");
    registry.add("biwenger.password", () -> "s3cret-password");
    registry.add("biwenger.min-request-interval", () -> "0s");
    registry.add("biwenger.initial-backoff", () -> "0s");
  }

  @Autowired DailySyncService sync;
  @Autowired MutableClock clock;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void freshDatabaseAndBiwenger() throws IOException {
    emptyTables();
    clock.set(MORNING);
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    stub("/api/v2/account", sample("account.json"));
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", sample("catalog.json"));
    stub("/api/v2/market", sample("market.json"));
    stub("/api/v2/rounds/league", sample("rounds-league.json"));
    // The points refresh (#78) runs inside the sync when a round has finished.
    biwenger.stubFor(
        get(urlPathMatching("/cdn/api/v2/players/la-liga/.*"))
            .willReturn(okJson(sample("player-reports.json"))));
  }

  /** The container is shared with other test classes, which count rows: leave nothing behind. */
  @AfterEach
  void emptyTables() {
    jdbc.execute(
        "TRUNCATE player, player_price_snapshot, market_snapshot, league_standing_snapshot, sync_run,"
            + " player_points_report, points_refresh");
  }

  @Test
  void storesTodaysSnapshotAndLogsASummary(CapturedOutput output) {
    SyncSummary summary = sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(summary.day()).isEqualTo(LocalDate.of(2026, 10, 22));
    assertThat(summary.players()).isEqualTo(39);
    assertThat(summary.marketListings()).isEqualTo(19);
    assertThat(summary.standings()).isEqualTo(6);
    assertThat(count("player")).isEqualTo(39);
    assertThat(count("player_price_snapshot")).isEqualTo(39);
    assertThat(count("market_snapshot")).isEqualTo(19);
    assertThat(count("league_standing_snapshot")).isEqualTo(6);
    assertThat(output)
        .contains(
            "Daily sync done, day 2026-10-22: 39 players, 19 market listings, 6 standings in");
  }

  @Test
  void runningTwiceOnTheSameDayDoesNotDuplicate() {
    sync.syncToday(SyncTrigger.SCHEDULED);
    clock.advance(Duration.ofHours(2));
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("player_price_snapshot")).isEqualTo(39);
    assertThat(count("market_snapshot")).isEqualTo(19);
    assertThat(count("league_standing_snapshot")).isEqualTo(6);
    // Every row was rewritten by the second run.
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM player_price_snapshot WHERE synced_at = ?",
                Long.class,
                java.sql.Timestamp.from(MORNING.plus(Duration.ofHours(2)))))
        .isEqualTo(39);
  }

  @Test
  void theNextDayAddsANewSnapshot() {
    sync.syncToday(SyncTrigger.SCHEDULED);
    clock.advance(Duration.ofDays(1));
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("player")).isEqualTo(39);
    assertThat(count("player_price_snapshot")).isEqualTo(78);
    assertThat(count("market_snapshot")).isEqualTo(38);
    assertThat(count("league_standing_snapshot")).isEqualTo(12);
  }

  @Test
  void aPlayerWhoLeftTheMarketSinceTheFirstRunIsNotKept() throws IOException {
    sync.syncToday(SyncTrigger.SCHEDULED);
    stub("/api/v2/market", withSales(sample("market.json"), 5));

    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("market_snapshot")).isEqualTo(5);
  }

  @Test
  void aManagerWhoLeftTheLeagueSinceTheFirstRunIsNotKept() throws IOException {
    sync.syncToday(SyncTrigger.SCHEDULED);
    stub("/api/v2/rounds/league", withStandings(sample("rounds-league.json"), 5));

    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("league_standing_snapshot")).isEqualTo(5);
  }

  @Test
  void aWriteThatFailsHalfwayStoresNothing() throws IOException {
    // The last listing breaks a database check (no negative prices), after the players were saved.
    stub("/api/v2/market", withLastSalePrice(sample("market.json"), -1));

    assertThatThrownBy(() -> sync.syncToday(SyncTrigger.SCHEDULED))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("player")).isZero();
    assertThat(count("player_price_snapshot")).isZero();
  }

  @Test
  void aBiwengerFailureStoresNothing() {
    biwenger.stubFor(get("/api/v2/market").willReturn(aResponse().withStatus(503)));

    assertThatThrownBy(() -> sync.syncToday(SyncTrigger.SCHEDULED))
        .isInstanceOf(BiwengerApiException.class)
        .hasMessageContaining("HTTP 503");
    // The catalog had already been read, but nothing is written until every call succeeded.
    assertThat(count("player")).isZero();
    assertThat(count("player_price_snapshot")).isZero();
  }

  @Test
  void recordsEveryRunWithItsResult() {
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(sync.latestRun())
        .get()
        .isInstanceOfSatisfying(
            SyncRun.Succeeded.class,
            run -> {
              assertThat(run.trigger()).isEqualTo(SyncTrigger.SCHEDULED);
              assertThat(run.day()).isEqualTo(LocalDate.of(2026, 10, 22));
              assertThat(run.players()).isEqualTo(39);
              assertThat(run.marketListings()).isEqualTo(19);
              assertThat(run.standings()).isEqualTo(6);
            });

    biwenger.stubFor(get("/api/v2/market").willReturn(aResponse().withStatus(503)));
    clock.advance(Duration.ofMinutes(5));
    assertThatThrownBy(() -> sync.syncToday(SyncTrigger.SCHEDULED));

    assertThat(sync.latestRun())
        .get()
        .isInstanceOfSatisfying(
            SyncRun.Failed.class, run -> assertThat(run.error()).contains("HTTP 503"));
    assertThat(count("sync_run")).isEqualTo(2);
  }

  @Test
  void onlyOneSyncRunsAtATime() throws InterruptedException {
    // A slow Biwenger keeps the first sync running.
    biwenger.stubFor(
        get("/api/v2/market")
            .willReturn(okJson(sampleUnchecked("market.json")).withFixedDelay(1_500)));
    sync.startInBackground();

    assertThat(sync.isRunning()).isTrue();
    // The run is recorded before startInBackground returns, so a status read already shows it.
    assertThat(sync.latestRun()).get().isInstanceOf(SyncRun.Running.class);
    assertThatThrownBy(() -> sync.syncToday(SyncTrigger.SCHEDULED))
        .isInstanceOf(SyncAlreadyRunningException.class);
    assertThatThrownBy(() -> sync.startInBackground())
        .isInstanceOf(SyncAlreadyRunningException.class);

    waitUntilIdle();
    assertThat(sync.latestRun()).get().extracting(SyncRun::result).isEqualTo(SyncResult.SUCCEEDED);
    assertThat(sync.latestRun()).get().extracting(SyncRun::trigger).isEqualTo(SyncTrigger.MANUAL);
    // After it finished, a new one can start.
    assertThat(sync.syncToday(SyncTrigger.SCHEDULED).players()).isEqualTo(39);
  }

  private void waitUntilIdle() throws InterruptedException {
    for (int i = 0; i < 100 && sync.isRunning(); i++) {
      Thread.sleep(100);
    }
    assertThat(sync.isRunning()).as("the background sync finished").isFalse();
  }

  private static String sampleUnchecked(String name) {
    try {
      return sample(name);
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  @Test
  void theSnapshotDayIsTheMadridDate() {
    // 23:30 UTC on the 22nd is already the 23rd in Madrid.
    clock.set(Instant.parse("2026-10-22T23:30:00Z"));

    assertThat(sync.syncToday(SyncTrigger.SCHEDULED).day()).isEqualTo(LocalDate.of(2026, 10, 23));
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private static void stub(String url, String body) {
    biwenger.stubFor(get(urlEqualTo(url)).willReturn(okJson(body)));
  }

  /** The market sample with only its first {@code keep} sales. */
  private static String withSales(String market, int keep) {
    JsonMapper json = JsonMapper.builder().build();
    ObjectNode root = (ObjectNode) json.readTree(market);
    ArrayNode sales = (ArrayNode) root.get("data").get("sales");
    while (sales.size() > keep) {
      sales.remove(sales.size() - 1);
    }
    return json.writeValueAsString(root);
  }

  private static String withStandings(String rounds, int keep) {
    JsonMapper json = JsonMapper.builder().build();
    ObjectNode root = (ObjectNode) json.readTree(rounds);
    ArrayNode standings = (ArrayNode) root.get("data").get("league").get("standings");
    while (standings.size() > keep) {
      standings.remove(standings.size() - 1);
    }
    return json.writeValueAsString(root);
  }

  private static String withLastSalePrice(String market, long price) {
    JsonMapper json = JsonMapper.builder().build();
    ObjectNode root = (ObjectNode) json.readTree(market);
    ArrayNode sales = (ArrayNode) root.get("data").get("sales");
    ((ObjectNode) sales.get(sales.size() - 1)).put("price", price);
    return json.writeValueAsString(root);
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = DailySyncTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
