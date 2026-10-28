package io.github.davidms.biwenger.app.market.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.market.domain.Market;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.MarketPlayer;
import io.github.davidms.biwenger.app.player.domain.PlayerProfile;
import io.github.davidms.biwenger.app.player.service.PlayerReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Today's market. The listings are read live, because a manager can put a player on sale at any
 * time; it is one small call, kept for {@link #LISTINGS_FRESH_FOR}. Status, value trend and points
 * come from the database, which the daily sync fills. A failure isn't kept, so the next read tries
 * again.
 */
@Service
public class MarketService {

  static final Duration LISTINGS_FRESH_FOR = Duration.ofMinutes(1);

  private final BiwengerGateway biwenger;
  private final PlayerReader players;
  private final Clock clock;
  private List<MarketListing> listings;
  private Instant listingsFreshUntil;

  MarketService(BiwengerGateway biwenger, PlayerReader players, Clock clock) {
    this.biwenger = biwenger;
    this.players = players;
    this.clock = clock;
  }

  public Market market() {
    List<MarketListing> onSale = listings();
    List<Long> ids = onSale.stream().map(MarketListing::playerId).toList();
    LocalDate today = LocalDate.now(clock);
    // Biwenger first, outside any transaction: its call can take a while.
    var stored = players.read(ids, today.minusDays(Market.TREND_DAYS - 1), 0);
    Map<Long, PlayerProfile> profiles =
        stored.profiles().stream()
            .collect(Collectors.toMap(PlayerProfile::id, Function.identity()));

    List<MarketPlayer> market =
        onSale.stream()
            .filter(listing -> profiles.containsKey(listing.playerId()))
            .map(
                listing ->
                    new MarketPlayer(
                        listing,
                        profiles.get(listing.playerId()),
                        stored.prices().getOrDefault(listing.playerId(), List.of()),
                        stored.seasonPoints().getOrDefault(listing.playerId(), 0)))
            .toList();
    List<Long> missing = ids.stream().filter(id -> !profiles.containsKey(id)).toList();
    return new Market(market, missing, players.teams());
  }

  private synchronized List<MarketListing> listings() {
    Instant now = clock.instant();
    if (listings == null || !now.isBefore(listingsFreshUntil)) {
      listings = biwenger.marketListings();
      listingsFreshUntil = now.plus(LISTINGS_FRESH_FOR);
    }
    return listings;
  }
}
