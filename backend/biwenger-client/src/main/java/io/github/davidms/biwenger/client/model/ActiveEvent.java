package io.github.davidms.biwenger.client.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A round in the catalog's {@code activeEvents}: the active round and the next one. {@code start}
 * (Unix seconds) is left out for a round already under way, as the sample's active round shows.
 */
public record ActiveEvent(long id, String name, String status, @Nullable Long start) {

  public ActiveEvent {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(status, "status");
  }
}
