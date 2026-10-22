package io.github.davidms.biwenger.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

class BiwengerClientAuthTest {

  // Plain-text HTTP/2 off: the JDK client tries an h2c upgrade on http://, which WireMock's Jetty
  // breaks for requests with a body. Biwenger itself is HTTPS, where HTTP/2 is negotiated properly.
  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .configureStaticDsl(true)
          .build();

  private static final String LOGIN = BiwengerSession.LOGIN_PATH;
  private static final String ACCOUNT = BiwengerClient.ACCOUNT_PATH;
  private static final String MARKET = "/api/v2/market";
  private static final BiwengerCredentials CREDENTIALS =
      new BiwengerCredentials("manager@example.com", "s3cret-password");

  // From docs/biwenger-api/samples/account.json (anonymised ids).
  private static final long LEAGUE_ID = 1_000_001;
  private static final long USER_ID = 2_000_001;

  private String baseUrl;

  @BeforeEach
  void setUp() {
    baseUrl = wireMock.baseUrl();
  }

  private BiwengerClient client(BiwengerCredentials credentials) {
    return new BiwengerClient(RestClient.create(baseUrl), credentials);
  }

  private static void loginReturns(String token) {
    stubFor(post(LOGIN).willReturn(okJson("{\"token\": \"" + token + "\"}")));
  }

  private static void accountReturnsSample() throws IOException {
    stubFor(get(ACCOUNT).willReturn(okJson(sample("account.json"))));
  }

  @Test
  void logsInAndUsesTheAccountsLaLigaLeague() throws IOException {
    loginReturns("token-1");
    accountReturnsSample();

    LeagueContext league = client(CREDENTIALS).league();

    assertThat(league).isEqualTo(new LeagueContext(LEAGUE_ID, USER_ID));
    verify(
        postRequestedFor(urlEqualTo(LOGIN))
            .withRequestBody(
                equalToJson(
                    "{\"email\": \"manager@example.com\", \"password\": \"s3cret-password\"}")));
    verify(
        getRequestedFor(urlEqualTo(ACCOUNT))
            .withHeader("Authorization", equalTo("Bearer token-1"))
            .withHeader("X-League", absent())
            .withHeader("X-User", absent()));
  }

  @Test
  void sendsTokenAndLeagueHeadersOnLeagueCalls() throws IOException {
    loginReturns("token-1");
    accountReturnsSample();
    stubFor(get(MARKET).willReturn(okJson("{\"status\": 200, \"data\": {}}")));

    client(CREDENTIALS).get(MARKET, Map.class);

    verify(
        getRequestedFor(urlEqualTo(MARKET))
            .withHeader("Authorization", equalTo("Bearer token-1"))
            .withHeader("X-League", equalTo(String.valueOf(LEAGUE_ID)))
            .withHeader("X-User", equalTo(String.valueOf(USER_ID)))
            .withHeader("X-Lang", equalTo("es"))
            .withHeader("Accept", equalTo("application/json")));
  }

  @Test
  void logsInOnlyOnceForManyCalls() throws IOException {
    loginReturns("token-1");
    accountReturnsSample();
    stubFor(get(MARKET).willReturn(okJson("{}")));
    BiwengerClient client = client(CREDENTIALS);

    client.get(MARKET, Map.class);
    client.get(MARKET, Map.class);
    client.league();

    verify(1, postRequestedFor(urlEqualTo(LOGIN)));
    verify(1, getRequestedFor(urlEqualTo(ACCOUNT)));
  }

  @Test
  void failsClearlyOnBadCredentialsWithoutShowingThem() {
    stubFor(post(LOGIN).willReturn(unauthorized().withBody("{\"message\": \"bad login\"}")));

    assertThatThrownBy(() -> client(CREDENTIALS).league())
        .isInstanceOf(BiwengerAuthenticationException.class)
        .hasMessageContaining("rejected the login (HTTP 401)")
        .hasMessageNotContaining("s3cret-password")
        .hasMessageNotContaining("manager@example.com")
        .hasNoCause();
  }

  @Test
  void failsWithoutCallingBiwengerWhenCredentialsAreMissing() {
    assertThatThrownBy(() -> client(new BiwengerCredentials(" ", null)).league())
        .isInstanceOf(BiwengerAuthenticationException.class)
        .hasMessageContaining("BIWENGER_EMAIL and BIWENGER_PASSWORD");

    verify(0, postRequestedFor(urlEqualTo(LOGIN)));
  }

  @Test
  void logsInAgainWhenTheTokenExpires() throws IOException {
    stubFor(
        post(LOGIN)
            .inScenario("login")
            .whenScenarioStateIs(STARTED)
            .willReturn(okJson("{\"token\": \"token-1\"}"))
            .willSetStateTo("logged in once"));
    stubFor(
        post(LOGIN)
            .inScenario("login")
            .whenScenarioStateIs("logged in once")
            .willReturn(okJson("{\"token\": \"token-2\"}")));
    accountReturnsSample();
    stubFor(
        get(MARKET)
            .withHeader("Authorization", equalTo("Bearer token-1"))
            .willReturn(unauthorized()));
    stubFor(
        get(MARKET)
            .withHeader("Authorization", equalTo("Bearer token-2"))
            .willReturn(okJson("{\"status\": 200, \"data\": {\"sales\": []}}")));

    Map<?, ?> market = client(CREDENTIALS).get(MARKET, Map.class);

    assertThat(market.get("status")).isEqualTo(200);
    verify(2, postRequestedFor(urlEqualTo(LOGIN)));
    verify(
        1,
        getRequestedFor(urlEqualTo(MARKET)).withHeader("Authorization", equalTo("Bearer token-2")));
  }

  @Test
  void givesUpWhenAFreshTokenIsRejectedToo() throws IOException {
    loginReturns("token-1");
    accountReturnsSample();
    stubFor(get(MARKET).willReturn(unauthorized()));

    assertThatThrownBy(() -> client(CREDENTIALS).get(MARKET, Map.class))
        .isInstanceOf(BiwengerAuthenticationException.class)
        .hasMessageContaining("fresh token");

    verify(2, postRequestedFor(urlEqualTo(LOGIN)));
    verify(2, getRequestedFor(urlEqualTo(MARKET)));
  }

  @Test
  void failsClearlyWhenTheAccountHasNoLaLigaLeague() {
    loginReturns("token-1");
    stubFor(
        get(ACCOUNT)
            .willReturn(
                okJson(
                    """
                    {"status": 200, "data": {"leagues": [
                      {"id": 10, "competition": "premier-league", "user": {"id": 20}}]}}
                    """)));

    assertThatThrownBy(() -> client(CREDENTIALS).league())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage("The Biwenger account must have exactly one La Liga league, but it has 0");
  }

  @Test
  void failsClearlyWhenTheAccountHasTwoLaLigaLeagues() {
    loginReturns("token-1");
    stubFor(
        get(ACCOUNT)
            .willReturn(
                okJson(
                    """
                    {"status": 200, "data": {"leagues": [
                      {"id": 10, "competition": "la-liga", "user": {"id": 20}},
                      {"id": 11, "competition": "la-liga", "user": {"id": 21}}]}}
                    """)));

    assertThatThrownBy(() -> client(CREDENTIALS).league())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessageEndingWith("but it has 2");
  }

  @Test
  void loginFailureWithoutTokenIsAnAuthenticationError() {
    stubFor(
        post(LOGIN)
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("{}")
                    .withHeader("Content-Type", "application/json")));

    assertThatThrownBy(() -> client(CREDENTIALS).league())
        .isInstanceOf(BiwengerAuthenticationException.class)
        .hasMessageContaining("sent no token");
  }

  private static String sample(String name) throws IOException {
    try (InputStream in = BiwengerClientAuthTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
