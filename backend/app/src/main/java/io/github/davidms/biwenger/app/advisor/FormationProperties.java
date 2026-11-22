package io.github.davidms.biwenger.app.advisor;

import io.github.davidms.biwenger.app.advisor.domain.ReferenceFormation;
import io.github.davidms.biwenger.app.player.domain.Position;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Starters per position in the reference formation, under {@code biwenger.advisor.formation.*}, for
 * example {@code BIWENGER_ADVISOR_FORMATION_DEFENDER=5}. Shared by the buy and sell advisors. The
 * defaults equal {@link ReferenceFormation#DEFAULT}.
 */
@ConfigurationProperties("biwenger.advisor.formation")
public record FormationProperties(
    @DefaultValue("1") int goalkeeper,
    @DefaultValue("4") int defender,
    @DefaultValue("4") int midfielder,
    @DefaultValue("2") int forward,
    @DefaultValue("0") int coach) {

  public ReferenceFormation toFormation() {
    return new ReferenceFormation(
        Map.of(
            Position.GOALKEEPER, goalkeeper,
            Position.DEFENDER, defender,
            Position.MIDFIELDER, midfielder,
            Position.FORWARD, forward,
            Position.COACH, coach));
  }
}
