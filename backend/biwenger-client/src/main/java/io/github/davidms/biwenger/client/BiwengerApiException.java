package io.github.davidms.biwenger.client;

/** A call to the Biwenger API failed, or its answer can't be used. The message says what failed. */
public class BiwengerApiException extends RuntimeException {

  public BiwengerApiException(String message) {
    super(message);
  }
}
