package io.github.davidms.biwenger.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real PostgreSQL for tests, the same image as Docker Compose. One container for the whole test
 * run: every Spring context that imports this shares it, and Flyway migrates it on first start.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  static {
    POSTGRES.start();
  }

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return POSTGRES;
  }
}
