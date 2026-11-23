package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The buy advisor's settings under {@code biwenger.advisor.buy.*}, for example {@code
 * BIWENGER_ADVISOR_BUY_LIMIT=5}. The defaults equal {@link BuyWeights#DEFAULTS}.
 */
@ConfigurationProperties("biwenger.advisor.buy")
public record BuyProperties(@DefaultValue("10") int limit) {

  /** The weights as the domain takes them; fails at startup on a value out of range. */
  public BuyWeights toWeights() {
    return new BuyWeights(limit);
  }
}
