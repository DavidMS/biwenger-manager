package io.github.davidms.biwenger.app.player.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerPriceSnapshotRepository
    extends JpaRepository<PlayerPriceSnapshotEntity, PriceSnapshotId> {

  /** A player's value over time, oldest first. */
  List<PlayerPriceSnapshotEntity> findByIdPlayerIdOrderByIdSnapshotDateAsc(long playerId);

  /** Every player on one day, by player id. */
  List<PlayerPriceSnapshotEntity> findByIdSnapshotDateOrderByIdPlayerIdAsc(LocalDate day);
}
