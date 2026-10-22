package io.github.davidms.biwenger.client.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricePointTest {

  @Test
  void decodesTheYymmddDate() {
    assertThat(PricePoint.fromJson(List.of(251022L, 6_730_000L)))
        .isEqualTo(new PricePoint(LocalDate.of(2025, 10, 22), 6_730_000));
    assertThat(PricePoint.fromJson(List.of(260101L, 1L)).date())
        .isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void rejectsAnythingButADateAndAPrice() {
    assertThatThrownBy(() -> PricePoint.fromJson(List.of(251022L)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PricePoint.fromJson(Arrays.asList(251022L, null)))
        .isInstanceOf(IllegalArgumentException.class);
    // Month 13 is not a date.
    assertThatThrownBy(() -> PricePoint.fromJson(List.of(251322L, 1L)))
        .isInstanceOf(java.time.DateTimeException.class);
  }
}
