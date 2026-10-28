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
import io.github.davidms.biwenger.app.dashboard.domain.Dashboard;
import io.github.davidms.biwenger.app.league.domain.ScheduledRound;
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
  void readsTheDashboard() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(get("/api/v2/rounds/league").willReturn(okJson(sample("rounds-league.json"))));
    biwenger.stubFor(
        get("/api/v2/competitions/la-liga/data?lang=es&score=1")
            .willReturn(okJson(sample("catalog.json"))));

    Dashboard dashboard = gateway.dashboard();

    assertThat(dashboard.balance()).isEqualTo(Money.euros(51_420_000));
    assertThat(dashboard.teamValue()).isEqualTo(Money.euros(22_340_000));
    assertThat(dashboard.position()).isEqualTo(6);
    assertThat(dashboard.rounds()).extracting(ScheduledRound::name).containsExactly("Jornada 10");
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BiwengerGatewayTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
