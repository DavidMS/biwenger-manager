package io.github.davidms.biwenger.client;

/**
 * The Biwenger login. Empty values are allowed, so the app can start without them; logging in then
 * fails with a clear message.
 */
public record BiwengerCredentials(String email, String password) {

  public BiwengerCredentials {
    email = email == null ? "" : email.strip();
    password = password == null ? "" : password;
  }

  boolean isComplete() {
    return !email.isEmpty() && !password.isEmpty();
  }

  /** Never prints the password, and not the email either: logs shouldn't identify the user. */
  @Override
  public String toString() {
    return "BiwengerCredentials[email=" + mask(email) + ", password=" + mask(password) + "]";
  }

  private static String mask(String value) {
    return value.isEmpty() ? "<empty>" : "<set>";
  }
}
