package io.github.davidms.biwenger.client.model;

/** A player in your squad: its id (the catalog key) and how you own it. */
public record SquadPlayer(long id, Ownership owner) {}
