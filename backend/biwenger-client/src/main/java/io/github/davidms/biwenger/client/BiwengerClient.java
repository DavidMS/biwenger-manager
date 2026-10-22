package io.github.davidms.biwenger.client;

import io.github.davidms.biwenger.client.model.Account;
import io.github.davidms.biwenger.client.model.AccountLeague;
import io.github.davidms.biwenger.client.model.BoardEntry;
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.League;
import io.github.davidms.biwenger.client.model.Market;
import io.github.davidms.biwenger.client.model.RoundStandings;
import io.github.davidms.biwenger.client.model.Squad;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * call.
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
  static final String LA_LIGA = "la-liga";

  /**
   * The client's own JSON settings, so they don't depend on the caller's: Biwenger adds fields
   * without notice, and an unknown field must never break a read.
   */
  private static final JsonMapper JSON =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private final RestClient accountApi;
  private final RestClient api;
  private LeagueContext leagueContext;

  /**
   * @param restClient configured by the caller with the base URL ({@code https://biwenger.as.com}),
   *     timeouts and User-Agent (ADR 0004)
   * @param credentials the Biwenger login
   */
  public BiwengerClient(RestClient restClient, BiwengerCredentials credentials) {
    RestClient base =
        restClient
            .mutate()
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-Lang", "es")
            .configureMessageConverters(
                converters ->
                    converters.withJsonConverter(new JacksonJsonHttpMessageConverter(JSON)))
            .build();
    BiwengerSession session = new BiwengerSession(base, credentials);
    this.accountApi = base.mutate().requestInterceptor(AuthInterceptor.tokenOnly(session)).build();
    this.api =
        base.mutate()
            .requestInterceptor(AuthInterceptor.withLeague(session, this::leagueContext))
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
      throw new BiwengerApiException("Calling Biwenger failed for GET " + path, e);
    }
    if (envelope == null || envelope.data() == null) {
      throw new BiwengerApiException("Biwenger sent no data for GET " + path);
    }
    return envelope.data();
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
