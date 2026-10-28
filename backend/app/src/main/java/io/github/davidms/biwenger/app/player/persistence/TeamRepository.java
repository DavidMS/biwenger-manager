package io.github.davidms.biwenger.app.player.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

  /** Every club by name; clubs with the same name by id. */
  List<TeamEntity> findAllByOrderByNameAscIdAsc();
}
