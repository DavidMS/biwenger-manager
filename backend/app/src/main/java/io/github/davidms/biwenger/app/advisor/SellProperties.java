package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.SellWeights;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The sell advisor's settings under {@code biwenger.advisor.sell.*}, for example {@code
 * BIWENGER_ADVISOR_SELL_MIN_SQUAD_SIZE=13}. The defaults equal {@link SellWeights#DEFAULTS}, and
 * what each one means is documented there.
 */
@ConfigurationProperties("biwenger.advisor.sell")
public record SellProperties(
    @DefaultValue("15") int minSquadSize,
    @DefaultValue("1") int backups,
    @DefaultValue("3") BigDecimal fallingPercent,
    @DefaultValue("2") int injuryMatches,
    @DefaultValue("0.5") BigDecimal lowValueShare,
    @DefaultValue("10") int limit) {

  /** The weights as the domain takes them; fails at startup on a value out of range. */
  public SellWeights toWeights() {
    return new SellWeights(
        minSquadSize, backups, fallingPercent, injuryMatches, lowValueShare, limit);
  }
}
