package io.github.davidms.biwenger.app.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ScoringPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AdvisorConfiguration.class);

  @Test
  void defaultsMatchTheDomainDefaults() {
    runner.run(
        context ->
            assertThat(context.getBean(ScoringWeights.class)).isEqualTo(ScoringWeights.DEFAULTS));
  }

  @Test
  void bindsEveryWeightFromConfiguration() {
    runner
        .withPropertyValues(
            "biwenger.advisor.scoring.form-matches=4",
            "biwenger.advisor.scoring.form-weight=0.7",
            "biwenger.advisor.scoring.venue-weight=0.25",
            "biwenger.advisor.scoring.venue-min-games=2",
            "biwenger.advisor.scoring.trend-days=3",
            "biwenger.advisor.scoring.status-multiplier.doubtful=0.6",
            "biwenger.advisor.scoring.status-multiplier.unknown=0.4")
        .run(
            context -> {
              ScoringWeights weights = context.getBean(ScoringWeights.class);
              assertThat(weights.formMatches()).isEqualTo(4);
              assertThat(weights.formWeight()).isEqualTo(new BigDecimal("0.7"));
              assertThat(weights.venueWeight()).isEqualTo(new BigDecimal("0.25"));
              assertThat(weights.venueMinGames()).isEqualTo(2);
              assertThat(weights.trendDays()).isEqualTo(3);
              assertThat(weights.statusMultipliers().doubtful()).isEqualTo(new BigDecimal("0.6"));
              assertThat(weights.statusMultipliers().unknown()).isEqualTo(new BigDecimal("0.4"));
              assertThat(weights.statusMultipliers().injured()).isEqualTo(BigDecimal.ZERO);
            });
  }

  @Test
  void refusesToStartWithAWeightOutOfRange() {
    runner
        .withPropertyValues("biwenger.advisor.scoring.form-weight=1.5")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("formWeight must be between 0 and 1"));
  }
}
