package io.github.davidms.biwenger.app.player.service;

import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import java.util.List;
import java.util.Map;

/**
 * What {@link PlayerReader#read} found, all from one transaction. Profiles are by id; maps are
 * keyed and ordered by player id, and a player with no rows is left out of them.
 */
public record StoredPlayers(
    List<PlayerProfile> profiles,
    Map<Long, List<DailyPrice>> prices,
    Map<Long, Integer> seasonPoints,
    Map<Long, List<MatchResult>> lastMatches) {

  public StoredPlayers {
    profiles = List.copyOf(profiles);
    prices = Map.copyOf(prices);
    seasonPoints = Map.copyOf(seasonPoints);
    lastMatches = Map.copyOf(lastMatches);
  }
}
