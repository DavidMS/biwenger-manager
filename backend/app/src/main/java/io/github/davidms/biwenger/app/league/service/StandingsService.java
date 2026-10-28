package io.github.davidms.biwenger.app.league.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.league.domain.LeagueTable;
import io.github.davidms.biwenger.app.league.domain.RivalSquad;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The league table and each manager's squad. The table is read live, because points change after
 * every round and team values every day; it is one small call, kept for {@link #TABLE_FRESH_FOR},
 * and it holds every manager's lineup, so a squad costs no extra call. Player details come from the
 * database. A failure isn't kept, so the next read tries again.
 */
@Service
public class StandingsService {

  static final Duration TABLE_FRESH_FOR = Duration.ofMinutes(1);

  private final BiwengerGateway biwenger;
  private final PlayerReader players;
  private final Clock clock;
  private LeagueTable table;
  private Instant tableFreshUntil;

  StandingsService(BiwengerGateway biwenger, PlayerReader players, Clock clock) {
    this.biwenger = biwenger;
    this.players = players;
    this.clock = clock;
  }

  public synchronized LeagueTable table() {
    Instant now = clock.instant();
    if (table == null || !now.isBefore(tableFreshUntil)) {
      table = biwenger.leagueTable();
      tableFreshUntil = now.plus(TABLE_FRESH_FOR);
    }
    return table;
  }

  /**
   * @throws ManagerNotFoundException when no manager with this id is in the league
   */
  public RivalSquad squad(long managerId) {
    var entry = table().entry(managerId).orElseThrow(() -> new ManagerNotFoundException(managerId));
    var ids = entry.squadPlayerIds();
    // Only the season points are needed: a trend of today alone, and no matches.
    var stored = players.read(ids, LocalDate.now(clock), 0);
    var members =
        stored.profiles().stream()
            .map(
                profile ->
                    new RivalSquad.Member(
                        profile, stored.seasonPoints().getOrDefault(profile.id(), 0)))
            .toList();
    Set<Long> found = stored.profiles().stream().map(PlayerProfile::id).collect(Collectors.toSet());
    return new RivalSquad(entry, members, ids.stream().filter(id -> !found.contains(id)).toList());
  }
}
