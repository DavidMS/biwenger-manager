package io.github.davidms.biwenger.client.model;

import org.jspecify.annotations.Nullable;

/**
 * A player on sale. {@code user} is the selling manager, or {@code null} for a free player sold by
 * the game. {@code extended} is only sent, as {@code true}, for a free player kept from the day
 * before (inferred).
 */
public record Sale(
    SalePlayer player,
    long price,
    long date,
    long until,
    @Nullable Boolean extended,
    @Nullable Manager user) {

  public boolean isFreePlayer() {
    return user == null;
  }

  public boolean isExtended() {
    return Boolean.TRUE.equals(extended);
  }
}
