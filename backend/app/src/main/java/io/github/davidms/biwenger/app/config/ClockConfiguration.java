package io.github.davidms.biwenger.app.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The app's clock, in La Liga's and Biwenger's time zone (backend/CLAUDE.md). Code that needs the
 * time or the day takes this bean, so tests can fix it.
 */
@Configuration
class ClockConfiguration {

  static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

  @Bean
  Clock clock() {
    return Clock.system(MADRID);
  }
}
