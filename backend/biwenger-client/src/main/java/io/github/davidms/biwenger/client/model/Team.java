package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/** A La Liga club. */
public record Team(long id, String name, String slug) {

  public Team {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
  }
}
