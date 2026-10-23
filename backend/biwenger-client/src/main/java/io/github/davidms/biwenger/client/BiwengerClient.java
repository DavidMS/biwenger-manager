package io.github.davidms.biwenger.client;

import io.github.davidms.biwenger.client.model.Account;
import io.github.davidms.biwenger.client.model.AccountLeague;
import io.github.davidms.biwenger.client.model.BoardEntry;
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.League;
import io.github.davidms.biwenger.client.model.Market;
import io.github.davidms.biwenger.client.model.PlayerPrices;
import io.github.davidms.biwenger.client.model.PlayerReports;
import io.github.davidms.biwenger.client.model.RoundStandings;
import io.github.davidms.biwenger.client.model.Squad;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.function.LongSupplier;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Client for the unofficial Biwenger API (read-only so far), one method per endpoint
 * (docs/biwenger-api/). It logs in on first use, keeps the token in memory, logs in again once when
 * the token expires, and sends the league headers of the account's single La Liga league with every
 * call. The public CDN calls ({@link #playerPrices}, {@link #playerReports}) carry no token.
 *
 * <p>Thread-safe. Create one per application.
 */
public final class BiwengerClient {

  static final String ACCOUNT_PATH = "/api/v2/account";
  static final String LEAGUE_PATH = "/api/v2/league/{leagueId}";
  static final String SQUAD_PATH = "/api/v2/user?fields=players(id,owner)";
  static final String MARKET_PATH = "/api/v2/market";
  static final String CATALOG_PATH = "/api/v2/competitions/la-liga/data?lang=es&score={scoreId}";
  static final String ROUNDS_PATH = "/api/v2/rounds/league";
  static final String BOARD_PATH = "/api/v2/league/{leagueId}/board?type=transfer,market";
  static final String PRICES_PATH = "/api/v2/players/la-liga/{slug}?lang=es&fields=*,prices";
  static final String REPORTS_PATH =
      "/api/v2/players/la-liga/{slug}?lang=es&fields="
          + "*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away),rawStats)";
  static final String LA_LIGA = "la-liga";

  /**
   * The client's own JSON settings, so they don't depend on the caller's: Biwenger adds fields
   * without notice, and an unknown field must never break a read.
   */
  private static final JsonMapper JSON =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private final RestClient accountApi;
  private final RestClient api;
  private final RestClient cdn;
  private LeagueContext leagueContext;

  /** With {@link BiwengerClientSettings#DEFAULTS}. */
  public BiwengerClient(
      RestClient restClient, RestClient cdnRestClient, BiwengerCredentials credentials) {
    this(restClient, cdnRestClient, credentials, BiwengerClientSettings.DEFAULTS);
  }

  /**
   * @param restClient configured by the caller with the API's base URL ({@code
   *     https://biwenger.as.com}), timeouts and User-Agent (ADR 0004)
   * @param cdnRestClient the same, for the public CDN ({@code https://cf.biwenger.com})
   * @param credentials the Biwenger login
   * @param settings rate limit and retries
   */
  public BiwengerClient(
      RestClient restClient,
      RestClient cdnRestClient,
      BiwengerCredentials credentials,
      BiwengerClientSettings settings) {
    this(restClient, cdnRestClient, credentials, settings, System::nanoTime, Pause.SLEEP);
  }

  /** Tests pass their own clock and pause, to check the waits without waiting. */
  BiwengerClient(
      RestClient restClient,
      RestClient cdnRestClient,
      BiwengerCredentials credentials,
      BiwengerClientSettings settings,
      LongSupplier nanoTime,
      Pause pause) {
    var retry = new RetryInterceptor(settings, pause);
    // One limiter for everything: API, CDN, login, re-logins and retries.
    var rateLimit =
        new RateLimitInterceptor(new RateLimiter(settings.minRequestInterval(), nanoTime, pause));
    BiwengerSession session =
        new BiwengerSession(configure(restClient, retry, null, rateLimit), credentials);
    this.accountApi = configure(restClient, retry, AuthInterceptor.tokenOnly(session), rateLimit);
    this.api =
        configure(
            restClient, retry, AuthInterceptor.withLeague(session, this::leagueContext), rateLimit);
    this.cdn = configure(cdnRestClient, retry, null, rateLimit);
  }

  /**
   * Adds the Biwenger headers, the client's JSON mapper and the interceptors in this order: retry
   * (outermost), auth if any, then the rate limit, so every request that reaches the network waits.
   */
  private static RestClient configure(
      RestClient restClient,
      ClientHttpRequestInterceptor retry,
      @Nullable ClientHttpRequestInterceptor auth,
      ClientHttpRequestInterceptor rateLimit) {
    return restClient
        .mutate()
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("X-Lang", "es")
        .configureMessageConverters(
            converters -> converters.withJsonConverter(new JacksonJsonHttpMessageConverter(JSON)))
        .requestInterceptors(
            interceptors -> {
              interceptors.add(retry);
              if (auth != null) {
                interceptors.add(auth);
              }
              interceptors.add(rateLimit);
            })
        .build();
  }

  /**
   * The account's La Liga league, read from the account on first use.
   *
   * @throws BiwengerApiException unless the account has exactly one La Liga league
   * @throws BiwengerAuthenticationException if the credentials are missing or wrong
   */
  public synchronized LeagueContext leagueContext() {
    if (leagueContext == null) {
      leagueContext = findLaLigaLeague(account());
    }
    return leagueContext;
  }

  /** The account, its leagues, your team in each and their rules. */
  public Account account() {
    return data(accountApi, ACCOUNT_PATH, new ParameterizedTypeReference<Envelope<Account>>() {});
  }

  /** The league and its managers. Standings are in {@link #roundStandings()}. */
  public League league() {
    return data(
        api,
        LEAGUE_PATH,
        new ParameterizedTypeReference<Envelope<League>>() {},
        leagueContext().leagueId());
  }

  /** Your players, with how you own each. */
  public Squad squad() {
    return data(api, SQUAD_PATH, new ParameterizedTypeReference<Envelope<Squad>>() {});
  }

  /** Today's market: your balance and maximum bid, and the players on sale. */
  public Market market() {
    return data(api, MARKET_PATH, new ParameterizedTypeReference<Envelope<Market>>() {});
  }

  /**
   * Every La Liga player and club, with points in the league's scoring system. Large (about 230 KB)
   * and updated about once a day, so fetch it sparingly.
   */
  public Catalog catalog() {
    return data(
        api,
        CATALOG_PATH,
        new ParameterizedTypeReference<Envelope<Catalog>>() {},
        leagueContext().scoreId());
  }

  /** The current round and the standings, with every manager's team value and lineup. */
  public RoundStandings roundStandings() {
    return data(api, ROUNDS_PATH, new ParameterizedTypeReference<Envelope<RoundStandings>>() {});
  }

  /** The league's latest transfers and auction results, newest first. */
  public List<BoardEntry> board() {
    return data(
        api,
        BOARD_PATH,
        new ParameterizedTypeReference<Envelope<List<BoardEntry>>>() {},
        leagueContext().leagueId());
  }

  /**
   * A player's daily value for about the last year, oldest first. Public CDN, no login.
   *
   * @param slug the player's {@code slug} from the catalog, such as {@code ayoze-perez}
   */
  public PlayerPrices playerPrices(String slug) {
    requireSlug(slug);
    return data(
        cdn, PRICES_PATH, new ParameterizedTypeReference<Envelope<PlayerPrices>>() {}, slug);
  }

  /**
   * A player's report for every club match this season: round, home or away, points per scoring
   * system, minutes, and why he didn't play when he didn't. Public CDN, no login.
   *
   * @param slug the player's {@code slug} from the catalog, such as {@code ayoze-perez}
   */
  public PlayerReports playerReports(String slug) {
    requireSlug(slug);
    return data(
        cdn, REPORTS_PATH, new ParameterizedTypeReference<Envelope<PlayerReports>>() {}, slug);
  }

  private static void requireSlug(String slug) {
    if (slug == null || slug.isBlank()) {
      throw new IllegalArgumentException("A player slug is required, such as ayoze-perez");
    }
  }

  private static <T> T data(
      RestClient client,
      String path,
      ParameterizedTypeReference<Envelope<T>> type,
      Object... uriVariables) {
    Envelope<T> envelope;
    try {
      envelope = client.get().uri(path, uriVariables).retrieve().body(type);
    } catch (RestClientResponseException e) {
      // Not chained as the cause: its message carries the response body, which can hold personal
      // data (the account) or anything else Biwenger sends.
      throw new BiwengerApiException(
          "Biwenger answered HTTP " + e.getStatusCode().value() + " to GET " + path);
    } catch (RestClientException e) {
      if (e.getCause() instanceof HttpMessageNotReadableException) {
        // A required field is missing or has another type: Biwenger probably changed its format.
        throw new BiwengerApiException(
            "Biwenger's answer to GET " + path + " doesn't match docs/biwenger-api/", e);
      }
      throw networkFailure("GET " + path, e);
    }
    if (envelope == null || envelope.data() == null) {
      throw new BiwengerApiException("Biwenger sent no data for GET " + path);
    }
    return envelope.data();
  }

  /** A failure without an HTTP answer: a timeout, or Biwenger couldn't be reached. */
  static BiwengerApiException networkFailure(String call, RestClientException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
        return new BiwengerApiException("Biwenger did not answer " + call + " in time", e);
      }
    }
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return new BiwengerApiException(
        "Could not reach Biwenger for " + call + " (" + root.getClass().getSimpleName() + ")", e);
  }

  private static LeagueContext findLaLigaLeague(Account account) {
    List<AccountLeague> laLiga =
        account.leagues().stream().filter(league -> LA_LIGA.equals(league.competition())).toList();
    if (laLiga.size() != 1) {
      throw new BiwengerApiException(
          "The Biwenger account must have exactly one La Liga league, but it has " + laLiga.size());
    }
    AccountLeague found = laLiga.getFirst();
    return new LeagueContext(found.id(), found.user().id(), found.scoreId());
  }
}
