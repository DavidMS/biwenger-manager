package io.github.davidms.biwenger.app.sync.persistence;

import io.github.davidms.biwenger.app.sync.domain.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRunRepository extends JpaRepository<SyncRunEntity, Long> {

  /** The latest run, by when it started. */
  Optional<SyncRunEntity> findFirstByOrderByStartedAtDescIdDesc();

  List<SyncRunEntity> findByResultAndStartedAtBefore(SyncResult result, Instant before);
}
