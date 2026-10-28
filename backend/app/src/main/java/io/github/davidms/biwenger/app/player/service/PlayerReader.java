package io.github.davidms.biwenger.app.player.service;

import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.app.player.persistence.PlayerEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPointsReportEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPointsReportRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository.LatestPoints;
import io.github.davidms.biwenger.app.player.persistence.PlayerRepository;
import io.github.davidms.biwenger.app.player.persistence.TeamEntity;
import io.github.davidms.biwenger.app.player.persistence.TeamRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the stored players and their history, so other features use this service, not the player
 * tables.
 */
@Service
public class PlayerReader {

  private final PlayerRepository players;
  private final PlayerPriceSnapshotRepository prices;
  private final PlayerPointsReportRepository reports;
  private final TeamRepository teams;

  PlayerReader(
      PlayerRepository players,
      PlayerPriceSnapshotRepository prices,
      PlayerPointsReportRepository reports,
      TeamRepository teams) {
    this.players = players;
    this.prices = prices;
    this.reports = reports;
    this.teams = teams;
  }

  /**
   * The stored players among {@code ids}, with their values since {@code pricesFrom} (oldest
   * first), season points as the latest daily sync had them, and last {@code matches} matches
   * (latest first). One transaction, so a sync committing meanwhile can't mix two days in one
   * answer.
   */
  @Transactional(readOnly = true)
  public StoredPlayers read(Collection<Long> ids, LocalDate pricesFrom, int matches) {
    if (matches < 0) {
      throw new IllegalArgumentException("Can't read a negative number of matches: " + matches);
    }
    if (ids.isEmpty()) {
      return new StoredPlayers(List.of(), Map.of(), Map.of(), Map.of()); // SQL has no empty IN ()
    }
    return new StoredPlayers(
        profiles(ids),
        pricesSince(ids, pricesFrom),
        seasonPoints(ids),
        matches == 0 ? Map.of() : lastMatches(ids, matches));
  }

  /** Every club we have stored, by name. */
  @Transactional(readOnly = true)
  public List<Team> teams() {
    return teams.findAllByOrderByNameAscIdAsc().stream().map(TeamEntity::toDomain).toList();
  }

  private List<PlayerProfile> profiles(Collection<Long> ids) {
    return players.findAllById(ids).stream()
        .map(PlayerEntity::toDomain)
        .sorted(Comparator.comparingLong(PlayerProfile::id))
        .toList();
  }

  private Map<Long, List<DailyPrice>> pricesSince(Collection<Long> ids, LocalDate from) {
    return prices.findSince(ids, from).stream()
        .collect(
            Collectors.groupingBy(
                snapshot -> snapshot.id().playerId(),
                TreeMap::new,
                Collectors.mapping(PlayerPriceSnapshotEntity::toDomain, Collectors.toList())));
  }

  private Map<Long, Integer> seasonPoints(Collection<Long> ids) {
    return prices.findLatestPoints(ids).stream()
        .collect(
            Collectors.toMap(
                LatestPoints::getPlayerId,
                LatestPoints::getPoints,
                (first, second) -> first, // never runs: DISTINCT ON gives one row per player
                TreeMap::new));
  }

  // Loads every match of these players to keep the last few: fine for a squad. For the whole
  // catalog, a row_number() window query would be better.
  private Map<Long, List<MatchResult>> lastMatches(Collection<Long> ids, int count) {
    return reports.findByIdPlayerIdInOrderByMatchDateDescIdMatchIdDesc(ids).stream()
        .collect(
            Collectors.groupingBy(
                report -> report.id().playerId(),
                TreeMap::new,
                Collectors.mapping(
                    PlayerPointsReportEntity::toDomain,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        matches ->
                            List.copyOf(matches.subList(0, Math.min(count, matches.size())))))));
  }
}
