package io.github.davidms.biwenger.client.model;

import org.jspecify.annotations.Nullable;

/** A manager (a team in the league), as it appears in users, sales, standings and the board. */
public record Manager(long id, String name, @Nullable String icon) {}
