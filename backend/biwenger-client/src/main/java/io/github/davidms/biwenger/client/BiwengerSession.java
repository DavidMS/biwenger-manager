package io.github.davidms.biwenger.client;

import io.github.davidms.biwenger.client.AuthRecords.LoginRequest;
import io.github.davidms.biwenger.client.AuthRecords.LoginResponse;
import java.lang.System.Logger.Level;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Logs in and keeps the token in memory. The token is never written anywhere or logged. */
final class BiwengerSession {

  static final String LOGIN_PATH = "/api/v2/auth/login";

  /** The statuses that mean wrong credentials. Others (429, 5xx) are not the user's fault. */
  private static final Set<Integer> REJECTED_LOGIN = Set.of(400, 401, 403);

  private static final System.Logger LOG = System.getLogger(BiwengerSession.class.getName());

  private final RestClient restClient;
  private final BiwengerCredentials credentials;
  private String token;

  /**
   * @param restClient a client without the auth interceptor, since login needs no token
   */
  BiwengerSession(RestClient restClient, BiwengerCredentials credentials) {
    this.restClient = restClient;
    this.credentials = credentials;
  }

  /** The current token, logging in first if there is none yet. */
  synchronized String token() {
    if (token == null) {
      token = login();
    }
    return token;
  }

  /**
   * Replaces a token that Biwenger rejected. If another thread already replaced it, returns that
   * token instead of logging in again.
   */
  synchronized String renew(String rejected) {
    if (token == null || token.equals(rejected)) {
      LOG.log(Level.INFO, "Biwenger rejected the token; logging in again");
      token = login();
    }
    return token;
  }

  private String login() {
    if (!credentials.isComplete()) {
      throw new BiwengerAuthenticationException(
          "Biwenger credentials are not set: define BIWENGER_EMAIL and BIWENGER_PASSWORD");
    }
    LoginResponse response;
    try {
      response =
          restClient
              .post()
              .uri(LOGIN_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .body(new LoginRequest(credentials.email(), credentials.password()))
              .retrieve()
              .body(LoginResponse.class);
    } catch (RestClientResponseException e) {
      // Not chained as the cause: its message carries the response body, which we haven't seen yet.
      int status = e.getStatusCode().value();
      if (REJECTED_LOGIN.contains(status)) {
        throw new BiwengerAuthenticationException(
            "Biwenger rejected the login (HTTP "
                + status
                + "): check BIWENGER_EMAIL and BIWENGER_PASSWORD");
      }
      throw new BiwengerApiException("Biwenger login failed (HTTP " + status + ")");
    } catch (RestClientException e) {
      throw new BiwengerApiException("Calling Biwenger failed for the login", e);
    }
    if (response == null || response.token() == null || response.token().isBlank()) {
      throw new BiwengerAuthenticationException("Biwenger accepted the login but sent no token");
    }
    LOG.log(Level.INFO, "Logged in to Biwenger");
    return response.token();
  }
}
