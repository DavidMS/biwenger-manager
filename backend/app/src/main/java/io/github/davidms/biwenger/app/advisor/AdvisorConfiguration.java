package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the advisor's configuration. */
@Configuration
@EnableConfigurationProperties(ScoringProperties.class)
public class AdvisorConfiguration {

  /** Built once at startup, so a weight out of range stops the app with a clear message. */
  @Bean
  ScoringWeights scoringWeights(ScoringProperties properties) {
    return properties.toWeights();
  }
}
