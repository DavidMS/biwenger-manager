package io.github.davidms.biwenger.app.sync;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.app.MutableClock;
import io.github.davidms.biwenger.app.Samples;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.backfill.service.BackfillService;
import io.github.davidms.biwenger.app.biwenger.BiwengerMapper;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import io.github.davidms.biwenger.app.sync.service.DailySyncService;
import io.github.davidms.biwenger.client.model.Catalog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The points refresh inside the daily sync (#78), against WireMock and PostgreSQL. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, PointsRefreshTest.FixedClock.class})
class PointsRefreshTest {

  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  static final Instant MORNING = Instant.parse("2026-10-22T05:30:00Z");
  static final String CDN = "/cdn/api/v2/players/la-liga/";
  static final int PLAYERS = 39;

  /** Finished rounds in the catalog sample. Round 5176 (J6, part 3) is the active one. */
  static final int FINISHED_ROUNDS = 11;

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
    registry.add("biwenger.max-attempts", () -> "1");
  }

  @Autowired DailySyncService sync;
  @Autowired MutableClock clock;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean BackfillService backfill;

  @BeforeEach
  void freshDatabaseAndBiwenger() throws IOException {
    emptyTables();
    clock.set(MORNING);
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    stub("/api/v2/account", sample("account.json"));
    stub("/api/v2/competitions/la-liga/data?lang=es&score=1", sample("catalog.json"));
    stub("/api/v2/market", sample("market.json"));
    stub("/api/v2/rounds/league", sample("rounds-league.json"));
    biwenger.stubFor(
        get(urlPathMatching(CDN + ".*")).willReturn(okJson(sample("player-reports.json"))));
  }

  @AfterEach
  void emptyTables() {
    jdbc.execute(
        "TRUNCATE player, player_price_snapshot, market_snapshot, league_standing_snapshot,"
            + " sync_run, player_points_report, points_refresh");
  }

  @Test
  void refreshesEveryPlayerWhenRoundsHaveFinished() {
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(cdnCalls()).isEqualTo(PLAYERS);
    assertThat(count("player_points_report")).isEqualTo(PLAYERS * 9L);
    assertThat(count("points_refresh")).isEqualTo(FINISHED_ROUNDS);
    // Only reports: the day's prices are the snapshot's, one per player (ADR 0005).
    assertThat(count("player_price_snapshot")).isEqualTo(PLAYERS);
  }

  @Test
  void doesNotRefreshTheSameRoundsTwice() {
    sync.syncToday(SyncTrigger.SCHEDULED);
    clock.advance(Duration.ofDays(1));
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(cdnCalls()).isEqualTo(PLAYERS);
    assertThat(count("player_points_report")).isEqualTo(PLAYERS * 9L);
  }

  @Test
  void aNewlyFinishedRoundTriggersARefresh() throws IOException {
    sync.syncToday(SyncTrigger.SCHEDULED);
    stub(
        "/api/v2/competitions/la-liga/data?lang=es&score=1",
        withRoundFinished(sample("catalog.json"), 5176));
    clock.advance(Duration.ofDays(4));

    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(cdnCalls()).isEqualTo(2 * PLAYERS);
    assertThat(count("points_refresh")).isEqualTo(FINISHED_ROUNDS + 1);
  }

  @Test
  void waitsWhileTheBackfillRuns() {
    when(backfill.isRunning()).thenReturn(true);

    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(cdnCalls()).isZero();
    assertThat(count("points_refresh")).isZero();
    assertThat(count("player_price_snapshot")).isEqualTo(PLAYERS); // the snapshot still happened

    when(backfill.isRunning()).thenReturn(false);
    clock.advance(Duration.ofDays(1));
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("points_refresh")).isEqualTo(FINISHED_ROUNDS);
  }

  @Test
  void aFailedPlayerKeepsTheRoundsPendingForTheNextSync() {
    String slug = firstSlug();
    biwenger.stubFor(get(urlPathEqualTo(CDN + slug)).willReturn(aResponse().withStatus(503)));

    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("points_refresh")).isZero();
    assertThat(count("player_points_report")).isEqualTo((PLAYERS - 1) * 9L);

    biwenger.removeStub(get(urlPathEqualTo(CDN + slug)));
    clock.advance(Duration.ofDays(1));
    sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(count("points_refresh")).isEqualTo(FINISHED_ROUNDS);
    assertThat(count("player_points_report")).isEqualTo(PLAYERS * 9L);
  }

  @Test
  void aBiwengerOutageStopsTheRefreshButNotTheSync() {
    biwenger.stubFor(get(urlPathMatching(CDN + ".*")).willReturn(aResponse().withStatus(503)));

    var summary = sync.syncToday(SyncTrigger.SCHEDULED);

    assertThat(summary.players()).isEqualTo(PLAYERS);
    assertThat(cdnCalls()).isEqualTo(3);
    assertThat(count("points_refresh")).isZero();
  }

  private int cdnCalls() {
    return biwenger.findAll(getRequestedFor(urlPathMatching(CDN + ".*"))).size();
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private static String firstSlug() {
    return BiwengerMapper.players(Samples.data("catalog.json", Catalog.class)).getFirst().slug();
  }

  private static String withRoundFinished(String catalog, long roundId) {
    JsonMapper json = JsonMapper.builder().build();
    ObjectNode root = (ObjectNode) json.readTree(catalog);
    for (var round : root.get("data").get("season").get("rounds")) {
      if (round.get("id").asLong() == roundId) {
        ((ObjectNode) round).put("status", "finished");
      }
    }
    return json.writeValueAsString(root);
  }

  private static void stub(String url, String body) {
    biwenger.stubFor(get(urlEqualTo(url)).willReturn(okJson(body)));
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = PointsRefreshTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
