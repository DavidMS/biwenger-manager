package io.github.davidms.biwenger.app.player.service;

import io.github.davidms.biwenger.app.player.domain.DailyPrice;
import io.github.davidms.biwenger.app.player.domain.MatchResult;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.persistence.PlayerEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPointsReportEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPointsReportRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotEntity;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository;
import io.github.davidms.biwenger.app.player.persistence.PlayerPriceSnapshotRepository.LatestPoints;
import io.github.davidms.biwenger.app.player.persistence.PlayerRepository;
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
 * tables. Maps are keyed and ordered by player id; a player with no rows is left out.
 */
@Service
public class PlayerReader {

  private final PlayerRepository players;
  private final PlayerPriceSnapshotRepository prices;
  private final PlayerPointsReportRepository reports;

  PlayerReader(
      PlayerRepository players,
      PlayerPriceSnapshotRepository prices,
      PlayerPointsReportRepository reports) {
    this.players = players;
    this.prices = prices;
    this.reports = reports;
  }

  /** The players we have stored, by id. */
  @Transactional(readOnly = true)
  public List<PlayerProfile> profiles(Collection<Long> ids) {
    return players.findAllById(ids).stream()
        .map(PlayerEntity::toDomain)
        .sorted(Comparator.comparingLong(PlayerProfile::id))
        .toList();
  }

  /** Each player's daily values since {@code from}, oldest first. */
  @Transactional(readOnly = true)
  public Map<Long, List<DailyPrice>> pricesSince(Collection<Long> ids, LocalDate from) {
    return prices.findSince(ids, from).stream()
        .collect(
            Collectors.groupingBy(
                snapshot -> snapshot.id().playerId(),
                TreeMap::new,
                Collectors.mapping(PlayerPriceSnapshotEntity::toDomain, Collectors.toList())));
  }

  /** Each player's season points, as the latest daily sync had them. */
  @Transactional(readOnly = true)
  public Map<Long, Integer> seasonPoints(Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of(); // SQL has no empty IN ()
    }
    return prices.findLatestPoints(ids).stream()
        .collect(
            Collectors.toMap(
                LatestPoints::getPlayerId,
                LatestPoints::getPoints,
                (first, second) -> first,
                TreeMap::new));
  }

  /** Each player's last {@code count} matches, latest first. */
  @Transactional(readOnly = true)
  public Map<Long, List<MatchResult>> lastMatches(Collection<Long> ids, int count) {
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
