package io.github.davidms.biwenger.app.biwenger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.davidms.biwenger.client.BiwengerApiException;
import io.github.davidms.biwenger.client.BiwengerClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The read timeout from {@code biwenger.read-timeout}, against a Biwenger that answers late. */
@SpringBootTest
class BiwengerTimeoutTest {

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
    registry.add("biwenger.read-timeout", () -> "300ms");
    registry.add("biwenger.max-attempts", () -> "2");
    registry.add("biwenger.initial-backoff", () -> "0s");
    registry.add("biwenger.min-request-interval", () -> "0s");
  }

  @Autowired BiwengerClient client;

  @Test
  void aSlowAnswerTimesOutIsRetriedAndThenReported() throws IOException {
    biwenger.stubFor(post("/api/v2/auth/login").willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get("/api/v2/account").willReturn(okJson(sample("account.json"))));
    biwenger.stubFor(
        get("/api/v2/market").willReturn(aResponse().withStatus(200).withFixedDelay(2_000)));

    assertThatThrownBy(() -> client.market())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage("Biwenger did not answer GET /api/v2/market in time");
    biwenger.verify(2, getRequestedFor(urlEqualTo("/api/v2/market")));
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BiwengerTimeoutTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
