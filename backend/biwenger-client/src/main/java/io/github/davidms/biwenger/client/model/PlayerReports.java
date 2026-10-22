package io.github.davidms.biwenger.client.model;

import java.util.List;
import java.util.Objects;

/**
 * {@code GET cf.biwenger.com/api/v2/players/la-liga/{slug}?fields=*,reports(...)}: one report per
 * club match this season, played or not, in date order (a postponed round comes when it was
 * played).
 */
public record PlayerReports(
    long id, String name, String slug, int position, List<MatchReport> reports) {

  public PlayerReports {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
    reports = reports == null ? List.of() : List.copyOf(reports);
  }
}
