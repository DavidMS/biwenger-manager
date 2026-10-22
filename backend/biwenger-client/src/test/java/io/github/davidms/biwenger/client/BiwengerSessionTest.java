package io.github.davidms.biwenger.client;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

class BiwengerSessionTest {

  // Plain-text HTTP/2 off: see BiwengerClientAuthTest.
  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  @Test
  void renewsARejectedTokenOnceEvenWhenTwoCallsReportIt() {
    biwenger.stubFor(
        post(BiwengerSession.LOGIN_PATH)
            .inScenario("login")
            .whenScenarioStateIs(STARTED)
            .willReturn(okJson("{\"token\": \"token-1\"}"))
            .willSetStateTo("renewed"));
    biwenger.stubFor(
        post(BiwengerSession.LOGIN_PATH)
            .inScenario("login")
            .whenScenarioStateIs("renewed")
            .willReturn(okJson("{\"token\": \"token-2\"}")));
    var session =
        new BiwengerSession(
            RestClient.create(biwenger.baseUrl()),
            new BiwengerCredentials("manager@example.com", "s3cret-password"));

    String first = session.token();
    // Two calls that both failed with token-1, as two threads would after it expired.
    String renewedOnce = session.renew(first);
    String renewedTwice = session.renew(first);

    assertThat(first).isEqualTo("token-1");
    assertThat(renewedOnce).isEqualTo("token-2");
    assertThat(renewedTwice).isEqualTo("token-2");
    biwenger.verify(2, postRequestedFor(urlEqualTo(BiwengerSession.LOGIN_PATH)));
  }
}
