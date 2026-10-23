package io.github.davidms.biwenger.app.league.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueStandingSnapshotRepository
    extends JpaRepository<LeagueStandingSnapshotEntity, StandingSnapshotId> {

  /** The league table on one day, by position. */
  List<LeagueStandingSnapshotEntity> findByIdSnapshotDateOrderByPositionAsc(LocalDate day);
}
