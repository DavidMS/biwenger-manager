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
import io.github.davidms.biwenger.client.BiwengerClient;
import io.github.davidms.biwenger.client.LeagueContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The client bean built from {@code biwenger.*} settings, against WireMock instead of Biwenger. */
@SpringBootTest
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
    registry.add("biwenger.email", () -> "manager@example.com");
    registry.add("biwenger.password", () -> "s3cret-password");
  }

  @Autowired BiwengerClient client;
  @Autowired BiwengerProperties properties;

  @Test
  void clientUsesTheConfiguredBaseUrlCredentialsAndUserAgent() {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(
        get("/api/v2/account")
            .willReturn(
                okJson(
                    """
                    {"status": 200, "data": {"leagues": [
                      {"id": 10, "competition": "la-liga", "user": {"id": 20}}]}}
                    """)));

    assertThat(client.league()).isEqualTo(new LeagueContext(10, 20));
    biwenger.verify(
        getRequestedFor(urlEqualTo("/api/v2/account"))
            .withHeader("User-Agent", equalTo(BiwengerConfiguration.USER_AGENT)));
  }

  @Test
  void propertiesNeverPrintTheLogin() {
    assertThat(properties.toString())
        .contains("email=<set>", "password=<set>")
        .doesNotContain("manager@example.com", "s3cret-password");
  }
}
