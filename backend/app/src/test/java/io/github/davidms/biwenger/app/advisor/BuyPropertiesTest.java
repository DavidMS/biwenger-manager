package io.github.davidms.biwenger.app.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.player.domain.Position;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BuyPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AdvisorConfiguration.class);

  @Test
  void defaultsMatchTheDomainDefaults() {
    runner.run(
        context -> {
          assertThat(context.getBean(BuyWeights.class)).isEqualTo(BuyWeights.DEFAULTS);
          assertThat(context.getBean(ReferenceFormation.class))
              .isEqualTo(ReferenceFormation.DEFAULT);
        });
  }

  @Test
  void bindsTheFormationAndTheLimit() {
    runner
        .withPropertyValues(
            "biwenger.advisor.formation.defender=5",
            "biwenger.advisor.formation.forward=1",
            "biwenger.advisor.formation.coach=1",
            "biwenger.advisor.buy.limit=5")
        .run(
            context -> {
              ReferenceFormation formation = context.getBean(ReferenceFormation.class);
              assertThat(formation.startersAt(Position.GOALKEEPER)).isEqualTo(1);
              assertThat(formation.startersAt(Position.DEFENDER)).isEqualTo(5);
              assertThat(formation.startersAt(Position.MIDFIELDER)).isEqualTo(4);
              assertThat(formation.startersAt(Position.FORWARD)).isEqualTo(1);
              assertThat(formation.startersAt(Position.COACH)).isEqualTo(1);
              assertThat(context.getBean(BuyWeights.class).limit()).isEqualTo(5);
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

  @Test
  void refusesToStartWithANegativeFormationCount() {
    runner
        .withPropertyValues("biwenger.advisor.formation.goalkeeper=-1")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("GOALKEEPER"));
  }
}
