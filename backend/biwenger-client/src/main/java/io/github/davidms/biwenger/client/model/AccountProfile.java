package io.github.davidms.biwenger.client.model;

/**
 * The account itself. Its id is not the {@code X-User} value: that is {@link LeagueMember#id()}.
 */
public record AccountProfile(long id, String name) {}
