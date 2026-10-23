package io.github.davidms.biwenger.app.market.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketSnapshotRepository
    extends JpaRepository<MarketSnapshotEntity, MarketSnapshotId> {

  /** The market on one day, by player id. */
  List<MarketSnapshotEntity> findByIdSnapshotDateOrderByIdPlayerIdAsc(LocalDate day);
}
