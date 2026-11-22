package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The scoring model's weights under {@code biwenger.advisor.scoring.*}, for example {@code
 * BIWENGER_ADVISOR_SCORING_FORM_WEIGHT=0.7}. The defaults equal {@link ScoringWeights#DEFAULTS},
 * and what each one means is documented there.
 */
@ConfigurationProperties("biwenger.advisor.scoring")
public record ScoringProperties(
    @DefaultValue("5") int formMatches,
    @DefaultValue("0.6") BigDecimal formWeight,
    @DefaultValue("0.5") BigDecimal venueWeight,
    @DefaultValue("3") int venueMinGames,
    @DefaultValue("7") int trendDays,
    @DefaultValue StatusMultiplier statusMultiplier) {

  /** Expected-points multiplier per player status. */
  public record StatusMultiplier(
      @DefaultValue("1") BigDecimal available,
      @DefaultValue("0.5") BigDecimal doubtful,
      @DefaultValue("0") BigDecimal injured,
      @DefaultValue("0") BigDecimal suspended,
      @DefaultValue("0.5") BigDecimal unknown) {}

  /** The weights as the domain takes them; fails at startup on a value out of range. */
  public ScoringWeights toWeights() {
    return new ScoringWeights(
        formMatches,
        formWeight,
        venueWeight,
        venueMinGames,
        trendDays,
        new ScoringWeights.StatusMultipliers(
            statusMultiplier.available(),
            statusMultiplier.doubtful(),
            statusMultiplier.injured(),
            statusMultiplier.suspended(),
            statusMultiplier.unknown()));
  }
}
