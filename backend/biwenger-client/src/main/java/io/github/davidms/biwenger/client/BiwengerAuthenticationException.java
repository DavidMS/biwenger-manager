package io.github.davidms.biwenger.client;

/**
 * Biwenger didn't accept our credentials or token: the credentials are missing or wrong, or a fresh
 * token was rejected too. The message never contains the email, password or token.
 */
public class BiwengerAuthenticationException extends BiwengerApiException {

  public BiwengerAuthenticationException(String message) {
    super(message);
  }
}
