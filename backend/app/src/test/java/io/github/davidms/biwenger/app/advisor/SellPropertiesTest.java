package io.github.davidms.biwenger.app.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.davidms.biwenger.app.advisor.domain.SellWeights;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SellPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AdvisorConfiguration.class);

  @Test
  void defaultsMatchTheDomainDefaults() {
    runner.run(
        context -> assertThat(context.getBean(SellWeights.class)).isEqualTo(SellWeights.DEFAULTS));
  }

  @Test
  void bindsEverySetting() {
    runner
        .withPropertyValues(
            "biwenger.advisor.sell.min-squad-size=13",
            "biwenger.advisor.sell.backups=2",
            "biwenger.advisor.sell.falling-percent=5",
            "biwenger.advisor.sell.injury-matches=3",
            "biwenger.advisor.sell.low-value-share=0.4",
            "biwenger.advisor.sell.limit=4")
        .run(
            context ->
                assertThat(context.getBean(SellWeights.class))
                    .isEqualTo(
                        new SellWeights(13, 2, new BigDecimal("5"), 3, new BigDecimal("0.4"), 4)));
  }

  @Test
  void refusesToStartWithASettingOutOfRange() {
    runner
        .withPropertyValues("biwenger.advisor.sell.low-value-share=2")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("lowValueShare must be between 0 and 1"));
  }
}
