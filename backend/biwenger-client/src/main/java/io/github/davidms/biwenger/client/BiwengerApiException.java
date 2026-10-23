package io.github.davidms.biwenger.client;

/**
 * A call to the Biwenger API failed, or its answer can't be used. The message says what failed.
 * {@link #isTransient()} says whether trying again later may work: a server error, a timeout or a
 * network failure may; a 4xx answer or an answer that doesn't match docs/biwenger-api/ won't.
 */
public class BiwengerApiException extends RuntimeException {

  private final boolean transientFailure;

  public BiwengerApiException(String message) {
    this(message, null, false);
  }

  public BiwengerApiException(String message, Throwable cause) {
    this(message, cause, false);
  }

  public BiwengerApiException(String message, Throwable cause, boolean transientFailure) {
    super(message, cause);
    this.transientFailure = transientFailure;
  }

  /** Whether the same call may succeed later without anything changing on our side. */
  public boolean isTransient() {
    return transientFailure;
  }

  /** A server error or "too many requests" may pass; another 4xx won't. */
  static boolean isTransientStatus(int status) {
    return status >= 500 || status == 429;
  }
}
