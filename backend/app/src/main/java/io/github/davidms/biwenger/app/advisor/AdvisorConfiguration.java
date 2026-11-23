package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import io.github.davidms.biwenger.app.advisor.domain.SellWeights;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the advisor's configuration. Each weights bean is built once at startup, so a value out of
 * range stops the app with a clear message.
 */
@Configuration
@EnableConfigurationProperties({
  ScoringProperties.class,
  FormationProperties.class,
  BuyProperties.class,
  SellProperties.class
})
public class AdvisorConfiguration {

  @Bean
  ScoringWeights scoringWeights(ScoringProperties properties) {
    return properties.toWeights();
  }

  @Bean
  ReferenceFormation referenceFormation(FormationProperties properties) {
    return properties.toFormation();
  }

  @Bean
  BuyWeights buyWeights(BuyProperties properties) {
    return properties.toWeights();
  }

  @Bean
  SellWeights sellWeights(SellProperties properties) {
    return properties.toWeights();
  }
}
