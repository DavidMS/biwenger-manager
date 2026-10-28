package io.github.davidms.biwenger.app.squad.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import io.github.davidms.biwenger.app.squad.domain.Squad;
import io.github.davidms.biwenger.app.squad.domain.SquadPlayer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Your squad. Which players you own is read live, because a won bid or a sale changes it during the
 * day; it is one small call, kept for {@link #IDS_FRESH_FOR}. Everything else (profile, value
 * trend, points, form) comes from the database, which the daily sync fills. A failure isn't kept,
 * so the next read tries again.
 */
@Service
public class SquadService {

  static final Duration IDS_FRESH_FOR = Duration.ofMinutes(1);

  private final BiwengerGateway biwenger;
  private final PlayerReader players;
  private final Clock clock;
  private List<Long> ids;
  private Instant idsFreshUntil;

  SquadService(BiwengerGateway biwenger, PlayerReader players, Clock clock) {
    this.biwenger = biwenger;
    this.players = players;
    this.clock = clock;
  }

  public Squad squad() {
    List<Long> owned = playerIds();
    LocalDate today = LocalDate.now(clock);
    var profiles = players.profiles(owned);
    var trends = players.pricesSince(owned, today.minusDays(Squad.TREND_DAYS - 1));
    var points = players.seasonPoints(owned);
    var form = players.lastMatches(owned, Squad.FORM_MATCHES);

    List<SquadPlayer> squad =
        profiles.stream()
            .map(
                profile ->
                    new SquadPlayer(
                        profile,
                        trends.getOrDefault(profile.id(), List.of()),
                        points.getOrDefault(profile.id(), 0),
                        form.getOrDefault(profile.id(), List.of())))
            .toList();
    Set<Long> stored = profiles.stream().map(PlayerProfile::id).collect(Collectors.toSet());
    return new Squad(squad, owned.stream().filter(id -> !stored.contains(id)).toList());
  }

  private synchronized List<Long> playerIds() {
    Instant now = clock.instant();
    if (ids == null || !now.isBefore(idsFreshUntil)) {
      ids = biwenger.squadPlayerIds();
      idsFreshUntil = now.plus(IDS_FRESH_FOR);
    }
    return ids;
  }
}
