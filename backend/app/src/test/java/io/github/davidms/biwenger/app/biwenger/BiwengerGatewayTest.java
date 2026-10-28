package io.github.davidms.biwenger.app.biwenger;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;
import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The gateway against WireMock. Its own class (and so its own Spring context): the client caches
 * the league once it knows it, which must not leak into tests that count account calls. Not
 * {@code @DirtiesContext}: closing a context stops the test container every later test shares. For
 * the same reason the cold-start test runs first: after it, the client knows the league.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BiwengerGatewayTest {

  // Plain-text HTTP/2 off: see BiwengerClientAuthTest in biwenger-client.
  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  @DynamicPropertySource
  static void biwengerSettings(DynamicPropertyRegistry registry) {
    registry.add("biwenger.base-url", biwenger::baseUrl);
    registry.add("biwenger.cdn-base-url", () -> biwenger.baseUrl() + "/cdn");
    registry.add("biwenger.email", () -> "manager@example.com");
    registry.add("biwenger.password", () -> "s3cret-password");
    registry.add("biwenger.min-request-interval", () -> "0s");
  }

  @Autowired BiwengerGateway gateway;

  @Test
  @Order(1)
  void readsTheConnectedAccount() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));

    assertThat(gateway.connectedAccount())
        .isEqualTo(new ConnectedAccount("Manager 1", "Manager 1", "Example League"));
    // A cold start reads the account twice: once to find the league, once for the names.
    assertThat(biwenger.findAll(getRequestedFor(urlEqualTo("/api/v2/account")))).hasSize(2);
  }

  @Test
  @Order(2)
  void readsTheTeamStatusAndTheScheduledRounds() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(get("/api/v2/rounds/league").willReturn(okJson(sample("rounds-league.json"))));
    biwenger.stubFor(
        get("/api/v2/competitions/la-liga/data?lang=es&score=1")
            .willReturn(okJson(sample("catalog.json"))));

    assertThat(gateway.teamStatus().teamValue()).isEqualTo(Money.euros(22_340_000));
    assertThat(gateway.scheduledRounds())
        .extracting(ScheduledRound::name)
        .containsExactly("Jornada 10");
  }

  @Test
  @Order(3)
  void readsTheSquadPlayerIds() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(
        get("/api/v2/user?fields=players(id,owner)").willReturn(okJson(sample("user-squad.json"))));

    assertThat(gateway.squadPlayerIds()).hasSize(13).startsWith(2001L, 2602L);
  }

  @Test
  @Order(4)
  void readsTodaysMarketByPlayerId() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(get("/api/v2/market").willReturn(okJson(sample("market.json"))));

    assertThat(gateway.marketListings()).hasSize(19).extracting(MarketListing::playerId).isSorted();
  }

  @Test
  @Order(5)
  void readsTheLeagueTableAndKnowsWhichManagerIsYou() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(get("/api/v2/rounds/league").willReturn(okJson(sample("rounds-league.json"))));

    var table = gateway.leagueTable();

    assertThat(table.entries()).hasSize(6);
    assertThat(table.yourManagerId()).isEqualTo(2_000_001);
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BiwengerGatewayTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
