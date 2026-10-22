package io.github.davidms.biwenger.client.model;

import org.jspecify.annotations.Nullable;

/** The player in a sale: its id, and how the seller owns it ({@code null} for free players). */
public record SalePlayer(long id, @Nullable Ownership owner) {}
