package io.github.davidms.biwenger.client.model;

/** A losing bid in an auction, visible on the board. */
public record Bid(Manager user, long amount) {}
