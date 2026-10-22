package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A round ("Jornada 6"). {@code status}: {@code finished}, {@code active} or {@code pending}. A
 * postponed round is a separate round with the same {@code shortName} and a {@code part}.
 */
public record Round(
    long id,
    String name,
    @JsonProperty("short") String shortName,
    String status,
    @Nullable Integer part) {

  public Round {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(shortName, "shortName");
    Objects.requireNonNull(status, "status");
  }
}
