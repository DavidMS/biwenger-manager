package io.github.davidms.biwenger.client;

import io.github.davidms.biwenger.client.AuthRecords.LoginRequest;
import io.github.davidms.biwenger.client.AuthRecords.LoginResponse;
import java.lang.System.Logger.Level;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Logs in and keeps the token in memory. The token is never written anywhere or logged. */
final class BiwengerSession {

  static final String LOGIN_PATH = "/api/v2/auth/login";

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
    } catch (HttpClientErrorException e) {
      // Not chained as the cause: its message carries the response body, which we haven't seen yet.
      throw new BiwengerAuthenticationException(
          "Biwenger rejected the login (HTTP "
              + e.getStatusCode().value()
              + "): check BIWENGER_EMAIL and BIWENGER_PASSWORD");
    }
    if (response == null || response.token() == null || response.token().isBlank()) {
      throw new BiwengerAuthenticationException("Biwenger accepted the login but sent no token");
    }
    LOG.log(Level.INFO, "Logged in to Biwenger");
    return response.token();
  }
}
