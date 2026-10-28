package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A player in the catalog. {@code position}: 1 goalkeeper, 2 defender, 3 midfielder, 4 forward, 5
 * coach. {@code status}: {@code "ok"}, {@code "injured"}, {@code "doubt"}; {@code statusInfo} and
 * {@code statusChange} only when it isn't {@code "ok"}. {@code fitness}: the last five rounds, most
 * recent first. {@code teamId} is {@code null} for a player with no club, such as one released
 * during the season.
 */
public record CatalogPlayer(
    long id,
    String name,
    String slug,
    @JsonProperty("teamID") @Nullable Long teamId,
    int position,
    List<Integer> altPositions,
    long price,
    long priceIncrement,
    String status,
    @Nullable String statusInfo,
    @Nullable Long statusChange,
    List<Fitness> fitness,
    int points,
    int playedHome,
    int playedAway,
    int pointsHome,
    int pointsAway,
    @Nullable Integer pointsLastSeason) {

  public CatalogPlayer {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(slug, "slug");
    Objects.requireNonNull(status, "status");
    altPositions = altPositions == null ? List.of() : List.copyOf(altPositions);
    // Biwenger writes null for a round the player didn't play; List.copyOf rejects nulls.
    fitness =
        fitness == null
            ? List.of()
            : fitness.stream().map(f -> f == null ? Fitness.NOT_PLAYED : f).toList();
  }
}
