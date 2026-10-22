package io.github.davidms.biwenger.client.model;

import org.jspecify.annotations.Nullable;

/**
 * How a manager owns a player. {@code price} (what they paid, inferred) is only sent in the market;
 * {@code clauseLockedUntil} only while the clause can't be paid yet.
 */
public record Ownership(
    long date, long clause, @Nullable Long price, @Nullable Long clauseLockedUntil) {}
