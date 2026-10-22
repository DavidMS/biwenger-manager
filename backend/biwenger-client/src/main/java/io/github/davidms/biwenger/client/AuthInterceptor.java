package io.github.davidms.biwenger.client;

import java.io.IOException;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds the token (and, for league calls, the league headers) to every call, and on a {@code 401}
 * logs in again once and retries. A second {@code 401} is an error, so a bad token can never cause
 * a login loop.
 */
final class AuthInterceptor implements ClientHttpRequestInterceptor {

  static final String LEAGUE_HEADER = "X-League";
  static final String USER_HEADER = "X-User";

  private final BiwengerSession session;
  private final @Nullable Supplier<LeagueContext> league;

  private AuthInterceptor(BiwengerSession session, @Nullable Supplier<LeagueContext> league) {
    this.session = session;
    this.league = league;
  }

  /** For the account call, which is where the league headers come from. */
  static AuthInterceptor tokenOnly(BiwengerSession session) {
    return new AuthInterceptor(session, null);
  }

  static AuthInterceptor withLeague(BiwengerSession session, Supplier<LeagueContext> league) {
    return new AuthInterceptor(session, league);
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (league != null) {
      LeagueContext context = league.get();
      request.getHeaders().set(LEAGUE_HEADER, String.valueOf(context.leagueId()));
      request.getHeaders().set(USER_HEADER, String.valueOf(context.userId()));
    }
    String token = session.token();
    request.getHeaders().setBearerAuth(token);
    ClientHttpResponse response = execution.execute(request, body);
    if (!isUnauthorized(response)) {
      return response;
    }
    response.close();
    request.getHeaders().setBearerAuth(session.renew(token));
    ClientHttpResponse retried = execution.execute(request, body);
    if (isUnauthorized(retried)) {
      retried.close();
      throw new BiwengerAuthenticationException(
          "Biwenger rejected a fresh token too (HTTP 401 after logging in again)");
    }
    return retried;
  }

  private static boolean isUnauthorized(ClientHttpResponse response) throws IOException {
    return response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED);
  }
}
