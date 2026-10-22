package io.github.davidms.biwenger.app.biwenger;

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
    @DefaultValue("5s") Duration connectTimeout,
    @DefaultValue("20s") Duration readTimeout) {

  /** A record's generated toString would print the password (backend/CLAUDE.md). */
  @Override
  public String toString() {
    return "BiwengerProperties[email="
        + mask(email)
        + ", password="
        + mask(password)
        + ", baseUrl="
        + baseUrl
        + ", connectTimeout="
        + connectTimeout
        + ", readTimeout="
        + readTimeout
        + "]";
  }

  private static String mask(String value) {
    return value == null || value.isEmpty() ? "<empty>" : "<set>";
  }
}
