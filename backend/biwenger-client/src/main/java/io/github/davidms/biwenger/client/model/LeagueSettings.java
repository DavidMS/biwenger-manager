package io.github.davidms.biwenger.client.model;

import java.util.Objects;

/**
 * The league rules the app needs. {@code balance} is {@code "hidden"} when rivals' cash is not
 * shown; {@code clause} is the clause mode ({@code "steal"} seen); {@code lineupMaxClubPlayers} and
 * {@code maxPurchasePrice} are 0 for no limit (inferred).
 */
public record LeagueSettings(
    String clause,
    int clauseIncrement,
    String balance,
    boolean marketShowBids,
    String userOffers,
    boolean lineupCaptain,
    boolean lineupCoach,
    boolean lineupReserves,
    boolean lineupMultiPos,
    int lineupMaxClubPlayers,
    long maxPurchasePrice) {

  public LeagueSettings {
    Objects.requireNonNull(clause, "clause");
    Objects.requireNonNull(balance, "balance");
    Objects.requireNonNull(userOffers, "userOffers");
  }
}
