package io.github.davidms.biwenger.client;

import java.io.IOException;
import java.util.function.Supplier;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds the token and the league headers to every call, and on a {@code 401} logs in again once and
 * retries. A second {@code 401} is an error, so a bad token can never cause a login loop.
 */
final class AuthInterceptor implements ClientHttpRequestInterceptor {

  static final String LEAGUE_HEADER = "X-League";
  static final String USER_HEADER = "X-User";

  private final BiwengerSession session;
  private final Supplier<LeagueContext> league;

  AuthInterceptor(BiwengerSession session, Supplier<LeagueContext> league) {
    this.session = session;
    this.league = league;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    // The account call is where the league headers come from, so it is sent without them.
    if (!BiwengerClient.ACCOUNT_PATH.equals(request.getURI().getPath())) {
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
