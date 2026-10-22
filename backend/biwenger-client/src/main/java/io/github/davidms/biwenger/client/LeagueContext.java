package io.github.davidms.biwenger.client;

/**
 * The account's La Liga league, sent as the {@code X-League} and {@code X-User} headers.
 *
 * @param leagueId the league id
 * @param userId your user id in that league, which is not the account id
 * @param scoreId the league's scoring system, which the catalog needs
 */
public record LeagueContext(long leagueId, long userId, int scoreId) {}
