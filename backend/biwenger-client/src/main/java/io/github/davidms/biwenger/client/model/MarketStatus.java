package io.github.davidms.biwenger.client.model;

/** Your balance and the highest bid you may place, which can exceed the balance. */
public record MarketStatus(long balance, long maximumBid) {}
