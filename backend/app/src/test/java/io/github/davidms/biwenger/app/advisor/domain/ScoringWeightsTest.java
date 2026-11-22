package io.github.davidms.biwenger.app.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.advisor.domain.ScoringWeights.StatusMultipliers;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoringWeightsTest {

  private static final ScoringWeights D = ScoringWeights.DEFAULTS;

  @Test
  void rejectsAFormWeightAboveOne() {
    assertThatThrownBy(
            () ->
                new ScoringWeights(
                    5, new BigDecimal("1.1"), D.venueWeight(), 3, 7, D.statusMultipliers()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("formWeight");
  }

  @Test
  void rejectsANegativeVenueWeight() {
    assertThatThrownBy(
            () ->
                new ScoringWeights(
                    5, D.formWeight(), new BigDecimal("-0.1"), 3, 7, D.statusMultipliers()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("venueWeight");
  }

  @Test
  void rejectsCountsBelowOne() {
    assertThatThrownBy(
            () ->
                new ScoringWeights(0, D.formWeight(), D.venueWeight(), 3, 7, D.statusMultipliers()))
        .hasMessageContaining("formMatches");
    assertThatThrownBy(
            () ->
                new ScoringWeights(5, D.formWeight(), D.venueWeight(), 0, 7, D.statusMultipliers()))
        .hasMessageContaining("venueMinGames");
    assertThatThrownBy(
            () ->
                new ScoringWeights(5, D.formWeight(), D.venueWeight(), 3, 0, D.statusMultipliers()))
        .hasMessageContaining("trendDays");
  }

  @Test
  void rejectsAStatusMultiplierAboveOne() {
    assertThatThrownBy(
            () ->
                new StatusMultipliers(
                    BigDecimal.ONE,
                    new BigDecimal("2"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO))
        .hasMessageContaining("doubtful");
  }

  @Test
  void looksUpTheMultiplierOfEveryStatus() {
    StatusMultipliers multipliers =
        new StatusMultipliers(
            new BigDecimal("1.0"),
            new BigDecimal("0.4"),
            new BigDecimal("0.1"),
            new BigDecimal("0.2"),
            new BigDecimal("0.3"));

    assertThat(multipliers.of(PlayerStatus.AVAILABLE)).isEqualTo(new BigDecimal("1.0"));
    assertThat(multipliers.of(PlayerStatus.DOUBTFUL)).isEqualTo(new BigDecimal("0.4"));
    assertThat(multipliers.of(PlayerStatus.INJURED)).isEqualTo(new BigDecimal("0.1"));
    assertThat(multipliers.of(PlayerStatus.SUSPENDED)).isEqualTo(new BigDecimal("0.2"));
    assertThat(multipliers.of(PlayerStatus.UNKNOWN)).isEqualTo(new BigDecimal("0.3"));
  }
}
