package io.github.davidms.biwenger.client.model;

/**
 * The match statistics, sent only when the player played. Only the minutes are mapped so far;
 * goals, assists, picas and others are in docs/biwenger-api/player-reports.md.
 */
public record RawStats(int minutesPlayed) {}
