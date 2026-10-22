package io.github.davidms.biwenger.client;

import io.github.davidms.biwenger.client.AuthRecords.AccountResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client for the unofficial Biwenger API (read-only so far). It logs in on first use, keeps the
 * token in memory, logs in again once when the token expires, and sends the league headers of the
 * account's single La Liga league with every call.
 *
 * <p>Thread-safe. Create one per application.
 */
public final class BiwengerClient {

  static final String ACCOUNT_PATH = "/api/v2/account";
  static final String LA_LIGA = "la-liga";

  private final RestClient accountApi;
  private final RestClient api;
  private LeagueContext league;

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
            .build();
    BiwengerSession session = new BiwengerSession(base, credentials);
    this.accountApi = base.mutate().requestInterceptor(AuthInterceptor.tokenOnly(session)).build();
    this.api =
        base.mutate().requestInterceptor(AuthInterceptor.withLeague(session, this::league)).build();
  }

  /**
   * The account's La Liga league, read from the account on first use.
   *
   * @throws BiwengerApiException unless the account has exactly one La Liga league
   * @throws BiwengerAuthenticationException if the credentials are missing or wrong
   */
  public synchronized LeagueContext league() {
    if (league == null) {
      league = findLaLigaLeague(fetch(accountApi, ACCOUNT_PATH, AccountResponse.class));
    }
    return league;
  }

  /** A GET with the token and league headers. The endpoint methods (issue 2.3) build on it. */
  <T> T get(String path, Class<T> type) {
    return fetch(api, path, type);
  }

  private static <T> T fetch(RestClient client, String path, Class<T> type) {
    try {
      return client.get().uri(path).retrieve().body(type);
    } catch (RestClientResponseException e) {
      // Not chained as the cause: its message carries the response body, which can hold personal
      // data (the account) or anything else Biwenger sends.
      throw new BiwengerApiException(
          "Biwenger answered HTTP " + e.getStatusCode().value() + " to GET " + path);
    } catch (RestClientException e) {
      throw new BiwengerApiException("Calling Biwenger failed for GET " + path, e);
    }
  }

  private static LeagueContext findLaLigaLeague(AccountResponse account) {
    List<AccountResponse.League> laLiga =
        Optional.ofNullable(account)
            .map(AccountResponse::data)
            .map(AccountResponse.Data::leagues)
            .orElse(List.of())
            .stream()
            .filter(league -> LA_LIGA.equals(league.competition()))
            .toList();
    if (laLiga.size() != 1) {
      throw new BiwengerApiException(
          "The Biwenger account must have exactly one La Liga league, but it has " + laLiga.size());
    }
    AccountResponse.League found = laLiga.getFirst();
    if (found.user() == null) {
      throw new BiwengerApiException("The Biwenger account response has no user in the league");
    }
    return new LeagueContext(found.id(), found.user().id());
  }
}
