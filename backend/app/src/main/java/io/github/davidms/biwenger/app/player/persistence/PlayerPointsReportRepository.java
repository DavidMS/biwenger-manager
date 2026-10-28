package io.github.davidms.biwenger.app.player.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerPointsReportRepository
    extends JpaRepository<PlayerPointsReportEntity, PointsReportId> {

  /** A player's matches, in the order they were played. */
  List<PlayerPointsReportEntity> findByIdPlayerIdOrderByMatchDateAsc(long playerId);

  /** These players' matches, latest first; matches on the same date by match id, latest first. */
  List<PlayerPointsReportEntity> findByIdPlayerIdInOrderByMatchDateDescIdMatchIdDesc(
      Collection<Long> playerIds);
}
