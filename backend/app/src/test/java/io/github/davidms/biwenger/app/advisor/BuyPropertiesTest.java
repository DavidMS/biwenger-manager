package io.github.davidms.biwenger.app.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import io.github.davidms.biwenger.app.player.domain.Position;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BuyPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AdvisorConfiguration.class);

  @Test
  void defaultsMatchTheDomainDefaults() {
    runner.run(
        context -> assertThat(context.getBean(BuyWeights.class)).isEqualTo(BuyWeights.DEFAULTS));
  }

  @Test
  void bindsTheFormationAndTheLimit() {
    runner
        .withPropertyValues(
            "biwenger.advisor.buy.starters.defender=5",
            "biwenger.advisor.buy.starters.forward=1",
            "biwenger.advisor.buy.starters.coach=1",
            "biwenger.advisor.buy.limit=5")
        .run(
            context -> {
              BuyWeights weights = context.getBean(BuyWeights.class);
              assertThat(weights.startersAt(Position.GOALKEEPER)).isEqualTo(1);
              assertThat(weights.startersAt(Position.DEFENDER)).isEqualTo(5);
              assertThat(weights.startersAt(Position.MIDFIELDER)).isEqualTo(4);
              assertThat(weights.startersAt(Position.FORWARD)).isEqualTo(1);
              assertThat(weights.startersAt(Position.COACH)).isEqualTo(1);
              assertThat(weights.limit()).isEqualTo(5);
            });
  }

  @Test
  void refusesToStartWithAZeroLimit() {
    runner
        .withPropertyValues("biwenger.advisor.buy.limit=0")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("limit must be at least 1"));
  }
}
