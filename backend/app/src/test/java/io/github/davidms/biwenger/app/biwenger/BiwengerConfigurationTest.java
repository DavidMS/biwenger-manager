package io.github.davidms.biwenger.app.biwenger;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.app.TestcontainersConfiguration;
import io.github.davidms.biwenger.client.BiwengerClient;
import io.github.davidms.biwenger.client.LeagueContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The client bean built from {@code biwenger.*} settings, against WireMock instead of Biwenger. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BiwengerConfigurationTest {

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

  @Autowired BiwengerClient client;
  @Autowired BiwengerProperties properties;

  @Test
  void clientUsesTheConfiguredBaseUrlCredentialsAndUserAgent() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));

    // The ids in the anonymised sample.
    assertThat(client.leagueContext()).isEqualTo(new LeagueContext(1_000_001, 2_000_001, 1));
    biwenger.verify(
        getRequestedFor(urlEqualTo("/api/v2/account"))
            .withHeader("User-Agent", equalTo(BiwengerConfiguration.USER_AGENT)));
  }

  @Test
  void cdnCallsUseTheConfiguredCdnBaseUrl() throws IOException {
    biwenger.stubFor(
        get("/cdn/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices")
            .willReturn(okJson(sample("player-prices.json"))));

    assertThat(client.playerPrices("ayoze-perez").prices()).isNotEmpty();
    biwenger.verify(
        getRequestedFor(
                urlEqualTo("/cdn/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices"))
            .withHeader("User-Agent", equalTo(BiwengerConfiguration.USER_AGENT)));
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BiwengerConfigurationTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void propertiesNeverPrintTheLogin() {
    assertThat(properties.toString())
        .contains("email=<set>", "password=<set>")
        .doesNotContain("manager@example.com", "s3cret-password");
  }
}
