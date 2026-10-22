package io.github.davidms.biwenger.client.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A team in a match, and its goals ({@code null} before the match). */
public record MatchTeam(long id, String name, String slug, @Nullable Integer score) {

  public MatchTeam {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
  }
}
