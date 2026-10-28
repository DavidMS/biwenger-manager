package io.github.davidms.biwenger.app.player.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlayerPriceSnapshotRepository
    extends JpaRepository<PlayerPriceSnapshotEntity, PriceSnapshotId> {

  /** A player's value over time, oldest first. */
  List<PlayerPriceSnapshotEntity> findByIdPlayerIdOrderByIdSnapshotDateAsc(long playerId);

  /** Every player on one day, by player id. */
  List<PlayerPriceSnapshotEntity> findByIdSnapshotDateOrderByIdPlayerIdAsc(LocalDate day);

  /** These players' values since {@code from}, by player id and then oldest first. */
  @Query(
      """
      select s from PlayerPriceSnapshotEntity s
      where s.id.playerId in :playerIds and s.id.snapshotDate >= :from
      order by s.id.playerId, s.id.snapshotDate
      """)
  List<PlayerPriceSnapshotEntity> findSince(Collection<Long> playerIds, LocalDate from);

  /**
   * Each player's season points from his latest snapshot that has them: a backfilled day has none.
   */
  @Query(
      value =
          """
          SELECT DISTINCT ON (player_id) player_id AS playerId, points
          FROM player_price_snapshot
          WHERE player_id IN (:playerIds) AND points IS NOT NULL
          ORDER BY player_id, snapshot_date DESC
          """,
      nativeQuery = true)
  List<LatestPoints> findLatestPoints(Collection<Long> playerIds);

  interface LatestPoints {
    long getPlayerId();

    int getPoints();
  }
}
