package io.github.davidms.biwenger.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.github.davidms.biwenger.client.model.Market;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

/** Rate limiting, retries with backoff and failure messages, with a fake clock: nothing waits. */
class BiwengerClientResilienceTest {

  // Plain-text HTTP/2 off: see BiwengerClientAuthTest.
  @RegisterExtension
  static WireMockExtension biwenger =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  private static final String LOGIN = BiwengerSession.LOGIN_PATH;
  private static final UrlPattern MARKET = urlEqualTo(BiwengerClient.MARKET_PATH);
  private static final String MARKET_JSON =
      "{\"status\": 200, \"data\": {\"status\": {\"balance\": 1, \"maximumBid\": 2}, \"sales\": []}}";
  private static final String PRICES =
      "/api/v2/players/la-liga/ayoze-perez?lang=es&fields=*,prices";

  /** Retries back off 100 ms, then 150 ms (200 capped); no gap between requests. */
  private static final BiwengerClientSettings RETRIES =
      new BiwengerClientSettings(Duration.ZERO, 3, Duration.ofMillis(100), Duration.ofMillis(150));

  /** Half a second between requests. */
  private static final BiwengerClientSettings RATE_LIMITED =
      new BiwengerClientSettings(Duration.ofMillis(500), 3, Duration.ZERO, Duration.ZERO);

  private final FakeTime time = new FakeTime();

  @BeforeEach
  void loginAndAccount() throws IOException {
    biwenger.stubFor(post(LOGIN).willReturn(okJson("{\"token\": \"t\"}")));
    biwenger.stubFor(get(BiwengerClient.ACCOUNT_PATH).willReturn(okJson(sample("account.json"))));
  }

  private BiwengerClient client(BiwengerClientSettings settings) {
    var restClient = RestClient.create(biwenger.baseUrl());
    return new BiwengerClient(
        restClient,
        restClient,
        new BiwengerCredentials("manager@example.com", "s3cret-password"),
        settings,
        time,
        time);
  }

  @Test
  void retriesAGetAfterServerErrorsWithExponentialBackoff() {
    marketAnswers(aResponse().withStatus(503), aResponse().withStatus(502), okJson(MARKET_JSON));

    Market market = client(RETRIES).market();

    assertThat(market.status().maximumBid()).isEqualTo(2);
    biwenger.verify(3, getRequestedFor(MARKET));
    assertThat(time.pauses).containsExactly(Duration.ofMillis(100), Duration.ofMillis(150));
  }

  @Test
  void givesUpAfterTheLastAttempt() {
    biwenger.stubFor(get(MARKET).willReturn(aResponse().withStatus(503)));

    assertThatThrownBy(() -> client(RETRIES).market())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage("Biwenger answered HTTP 503 to GET /api/v2/market");

    biwenger.verify(3, getRequestedFor(MARKET));
    assertThat(time.pauses).hasSize(2);
  }

  @Test
  void retriesNetworkErrors() {
    marketAnswers(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER), okJson(MARKET_JSON), null);

    assertThat(client(RETRIES).market().status().balance()).isEqualTo(1);
    biwenger.verify(2, getRequestedFor(MARKET));
  }

  @Test
  void reportsANetworkErrorThatOutlastsTheRetries() {
    biwenger.stubFor(get(MARKET).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    assertThatThrownBy(() -> client(RETRIES).market())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessageStartingWith("Could not reach Biwenger for GET /api/v2/market (")
        .hasCauseInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    // Our 3 attempts are 2 backoffs. On the wire there are up to 6 requests: the JDK's HTTP client
    // itself sends a GET again once when the connection is reset, as for a stale pooled connection.
    assertThat(time.pauses).hasSize(2);
  }

  @Test
  void doesNotRetryClientErrors() {
    biwenger.stubFor(get(MARKET).willReturn(aResponse().withStatus(429)));

    assertThatThrownBy(() -> client(RETRIES).market())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage("Biwenger answered HTTP 429 to GET /api/v2/market");

    biwenger.verify(1, getRequestedFor(MARKET));
    assertThat(time.pauses).isEmpty();
  }

  @Test
  void neverRepeatsAPost() {
    // Writes (Phase 6) are POSTs too: sending a bid twice must be impossible.
    biwenger.stubFor(post(LOGIN).willReturn(aResponse().withStatus(503)));

    assertThatThrownBy(() -> client(RETRIES).market())
        .isInstanceOf(BiwengerApiException.class)
        .hasMessage("Biwenger login failed (HTTP 503)");

    biwenger.verify(1, postRequestedFor(urlEqualTo(LOGIN)));
  }

  @Test
  void spacesEveryRequestIncludingTheLogin() {
    biwenger.stubFor(get(MARKET).willReturn(okJson(MARKET_JSON)));
    BiwengerClient client = client(RATE_LIMITED);

    client.market(); // login, account, market
    client.market();

    // The first request goes at once; each later one waits for its turn.
    assertThat(time.pauses).containsExactly(ms(500), ms(500), ms(500));
  }

  @Test
  void theApiAndTheCdnShareTheLimit() throws IOException {
    biwenger.stubFor(get(MARKET).willReturn(okJson(MARKET_JSON)));
    biwenger.stubFor(get(urlEqualTo(PRICES)).willReturn(okJson(sample("player-prices.json"))));
    BiwengerClient client = client(RATE_LIMITED);

    client.playerPrices("ayoze-perez");
    client.market();

    assertThat(time.pauses).containsExactly(ms(500), ms(500), ms(500));
  }

  @Test
  void retriesWaitTheirTurnToo() {
    marketAnswers(aResponse().withStatus(503), okJson(MARKET_JSON), null);

    client(RATE_LIMITED).market(); // login, account, market (503), market again

    assertThat(time.pauses).containsExactly(ms(500), ms(500), ms(500));
  }

  @Test
  void anInterruptedThreadStopsInsteadOfRetrying() {
    biwenger.stubFor(get(MARKET).willReturn(aResponse().withStatus(503)));
    BiwengerClient client = client(RETRIES);
    client.leagueContext(); // log in and read the account first

    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(client::market).isInstanceOf(BiwengerApiException.class);
    } finally {
      Thread.interrupted(); // clear the flag for the next test
    }

    // The JDK client won't even send on an interrupted thread; what matters is no retry and no
    // backoff. (The fake pause ignores interrupts, so before the fix the loop paused twice.)
    biwenger.verify(0, getRequestedFor(MARKET));
    assertThat(time.pauses).isEmpty();
  }

  @Test
  void aConnectTimeoutIsReportedAsATimeout() {
    // An end-to-end connect timeout needs an unroutable address, which is slow and flaky in CI.
    var failure =
        new org.springframework.web.client.ResourceAccessException(
            "I/O error", new java.net.http.HttpConnectTimeoutException("connect timed out"));

    assertThat(BiwengerClient.networkFailure("GET /api/v2/market", failure))
        .hasMessage("Biwenger did not answer GET /api/v2/market in time")
        .hasCause(failure);
  }

  @Test
  void saysWhichFailuresMayPassByThemselves() {
    BiwengerClient client = client(RETRIES);
    record Case(
        com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder answer,
        boolean transientFailure) {}
    for (var c :
        List.of(
            new Case(aResponse().withStatus(503), true),
            new Case(aResponse().withStatus(429), true),
            new Case(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER), true),
            new Case(aResponse().withStatus(404), false),
            // A required field missing: Biwenger changed its format, retrying won't help.
            new Case(okJson("{\"status\": 200, \"data\": {\"sales\": []}}"), false))) {
      biwenger.stubFor(get(MARKET).willReturn(c.answer()));

      assertThatThrownBy(client::market)
          .isInstanceOfSatisfying(
              BiwengerApiException.class,
              e -> assertThat(e.isTransient()).as(e.getMessage()).isEqualTo(c.transientFailure()));
    }
  }

  /** Market answers in order; the last one repeats. */
  private static void marketAnswers(
      com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder first,
      com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder second,
      com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder third) {
    biwenger.stubFor(
        get(MARKET)
            .inScenario("market")
            .whenScenarioStateIs(STARTED)
            .willReturn(first)
            .willSetStateTo("second"));
    biwenger.stubFor(
        get(MARKET)
            .inScenario("market")
            .whenScenarioStateIs("second")
            .willReturn(second)
            .willSetStateTo(third == null ? "second" : "third"));
    if (third != null) {
      biwenger.stubFor(
          get(MARKET).inScenario("market").whenScenarioStateIs("third").willReturn(third));
    }
  }

  private static Duration ms(long millis) {
    return Duration.ofMillis(millis);
  }

  private static String sample(String name) throws IOException {
    try (InputStream in =
        BiwengerClientResilienceTest.class.getResourceAsStream("/samples/" + name)) {
      assertThat(in).as("sample %s on the test classpath", name).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** A clock that only moves when something pauses, and remembers every pause. */
  static final class FakeTime implements LongSupplier, Pause {

    final List<Duration> pauses = new ArrayList<>();
    private long nanos;

    @Override
    public synchronized long getAsLong() {
      return nanos;
    }

    @Override
    public synchronized void pause(Duration duration) {
      pauses.add(duration);
      nanos += duration.toNanos();
    }

    synchronized void advance(Duration duration) {
      nanos += duration.toNanos();
    }
  }
}
