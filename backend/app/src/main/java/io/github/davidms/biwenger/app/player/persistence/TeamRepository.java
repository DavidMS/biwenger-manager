package io.github.davidms.biwenger.app.player.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

  /**
   * The clubs the latest sync saw, by name, then id. The sync writes them all with one time, so a
   * club relegated since is left out; its row stays for older players' history.
   */
  @Query(
      """
      select t from TeamEntity t
      where t.updatedAt = (select max(u.updatedAt) from TeamEntity u)
      order by t.name, t.id
      """)
  List<TeamEntity> findCurrent();
}
