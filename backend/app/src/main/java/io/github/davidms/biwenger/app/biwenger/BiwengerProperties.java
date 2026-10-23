package io.github.davidms.biwenger.app.biwenger;

import io.github.davidms.biwenger.client.BiwengerClientSettings;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code biwenger.*}. The login comes from the environment ({@code BIWENGER_EMAIL},
 * {@code BIWENGER_PASSWORD}); it may be empty, and then only calls to Biwenger fail.
 */
@ConfigurationProperties("biwenger")
public record BiwengerProperties(
    String email,
    String password,
    @DefaultValue("https://biwenger.as.com") URI baseUrl,
    @DefaultValue("https://cf.biwenger.com") URI cdnBaseUrl,
    @DefaultValue("5s") Duration connectTimeout,
    @DefaultValue("20s") Duration readTimeout,
    @DefaultValue("500ms") Duration minRequestInterval,
    @DefaultValue("3") int maxAttempts,
    @DefaultValue("1s") Duration initialBackoff,
    @DefaultValue("10s") Duration maxBackoff) {

  /** Rate limit and retries for the client (see {@link BiwengerClientSettings}). */
  public BiwengerClientSettings clientSettings() {
    return new BiwengerClientSettings(minRequestInterval, maxAttempts, initialBackoff, maxBackoff);
  }

  /** A record's generated toString would print the password (backend/CLAUDE.md). */
  @Override
  public String toString() {
    return "BiwengerProperties[email="
        + mask(email)
        + ", password="
        + mask(password)
        + ", baseUrl="
        + baseUrl
        + ", cdnBaseUrl="
        + cdnBaseUrl
        + ", connectTimeout="
        + connectTimeout
        + ", readTimeout="
        + readTimeout
        + ", minRequestInterval="
        + minRequestInterval
        + ", maxAttempts="
        + maxAttempts
        + ", initialBackoff="
        + initialBackoff
        + ", maxBackoff="
        + maxBackoff
        + "]";
  }

  private static String mask(String value) {
    return value == null || value.isEmpty() ? "<empty>" : "<set>";
  }
}
