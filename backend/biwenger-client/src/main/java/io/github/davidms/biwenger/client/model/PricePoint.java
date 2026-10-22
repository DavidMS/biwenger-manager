package io.github.davidms.biwenger.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A player's value on one day. Biwenger sends {@code [YYMMDD, price]}, for example {@code [251022,
 * 6730000]} for 2025-10-22. The date is decoded here because that encoding has one meaning only.
 */
public record PricePoint(LocalDate date, long price) {

  public PricePoint {
    Objects.requireNonNull(date, "date");
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static PricePoint fromJson(List<Long> pair) {
    if (pair == null || pair.size() != 2 || pair.get(0) == null || pair.get(1) == null) {
      throw new IllegalArgumentException("Expected [YYMMDD, price] but got " + pair);
    }
    long yymmdd = pair.get(0);
    // Six digits only: a switch to YYYYMMDD must fail, not decode to the year 4025.
    if (yymmdd < 0 || yymmdd > 991_231) {
      throw new IllegalArgumentException("Expected a YYMMDD date but got " + yymmdd);
    }
    var date =
        LocalDate.of(
            2000 + (int) (yymmdd / 10_000), (int) (yymmdd / 100 % 100), (int) (yymmdd % 100));
    return new PricePoint(date, pair.get(1));
  }
}
