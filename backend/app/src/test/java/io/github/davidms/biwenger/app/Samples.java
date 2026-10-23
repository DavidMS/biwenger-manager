package io.github.davidms.biwenger.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** The anonymised samples from docs/biwenger-api/samples/, read as the client would read them. */
public final class Samples {

  // The client's own setting (BiwengerClient), stated here too so the two can't drift apart.
  private static final JsonMapper JSON =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private Samples() {}

  /** The {@code data} of a sample, as a client record. */
  public static <T> T data(String sample, Class<T> type) {
    try (InputStream in = Samples.class.getResourceAsStream("/samples/" + sample)) {
      if (in == null) {
        throw new IllegalStateException("Sample " + sample + " is not on the test classpath");
      }
      return JSON.treeToValue(JSON.readTree(in).get("data"), type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
