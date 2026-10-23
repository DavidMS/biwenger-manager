package io.github.davidms.biwenger.app.backfill;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.backfill.service.BackfillService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The backfill goes through the client's rate limiter (#18): no two requests closer than the gap.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BackfillRateLimitTest {

  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  static final long GAP_MS = 25;

  @DynamicPropertySource
  static void biwengerSettings(DynamicPropertyRegistry registry) {
    registry.add("biwenger.base-url", biwenger::baseUrl);
    registry.add("biwenger.cdn-base-url", () -> biwenger.baseUrl() + "/cdn");
    registry.add("biwenger.email", () -> "manager@example.com");
    registry.add("biwenger.password", () -> "s3cret-password");
    registry.add("biwenger.min-request-interval", () -> GAP_MS + "ms");
  }

  @Autowired BackfillService backfill;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void emptyTables() {
    jdbc.execute("TRUNCATE player_price_snapshot, player_points_report, backfill_progress, player");
  }

  @Test
  void spacesEveryRequest() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(
        get(urlEqualTo("/api/v2/competitions/la-liga/data?lang=es&score=1"))
            .willReturn(okJson(sample("catalog.json"))));
    biwenger.stubFor(
        get(urlPathMatching("/cdn/.*"))
            .withQueryParam("fields", equalTo("*,prices"))
            .willReturn(okJson(sample("player-prices.json"))));
    biwenger.stubFor(
        get(urlPathMatching("/cdn/.*"))
            .withQueryParam("fields", matching("\\*,reports\\(.*"))
            .willReturn(okJson(sample("player-reports.json"))));

    backfill.run();

    // The limiter spaces the *starts* of requests in the client. WireMock only sees when each one
    // *arrived*, and a request a few ms slower on the wire shortens the gap after the previous one,
    // so single gaps can't be checked. The total can: 80 gaps can't fit in less than 80 × the gap,
    // give or take the first and last request's own delay.
    List<LoggedRequest> requests =
        biwenger.getAllServeEvents().stream()
            .map(event -> event.getRequest())
            .sorted(Comparator.comparing(LoggedRequest::getLoggedDate))
            .toList();
    assertThat(requests).hasSize(3 + 2 * 39);
    long span =
        requests.getLast().getLoggedDate().getTime()
            - requests.getFirst().getLoggedDate().getTime();
    assertThat(span).isGreaterThanOrEqualTo((requests.size() - 1) * GAP_MS - 20);
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BackfillRateLimitTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
