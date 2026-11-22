package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.BuyWeights;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The buy advisor's settings under {@code biwenger.advisor.buy.*}, for example {@code
 * BIWENGER_ADVISOR_BUY_STARTERS_DEFENDER=5}. The defaults equal {@link BuyWeights#DEFAULTS}.
 */
@ConfigurationProperties("biwenger.advisor.buy")
public record BuyProperties(@DefaultValue Starters starters, @DefaultValue("10") int limit) {

  /** Starters per position in the reference formation. */
  public record Starters(
      @DefaultValue("1") int goalkeeper,
      @DefaultValue("4") int defender,
      @DefaultValue("4") int midfielder,
      @DefaultValue("2") int forward,
      @DefaultValue("0") int coach) {}

  /** The weights as the domain takes them; fails at startup on a value out of range. */
  public BuyWeights toWeights() {
    return new BuyWeights(
        Map.of(
            Position.GOALKEEPER, starters.goalkeeper(),
            Position.DEFENDER, starters.defender(),
            Position.MIDFIELDER, starters.midfielder(),
            Position.FORWARD, starters.forward(),
            Position.COACH, starters.coach()),
        limit);
  }
}
