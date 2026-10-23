package io.github.davidms.biwenger.app.biwenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.davidms.biwenger.app.common.Money;
import io.github.davidms.biwenger.app.league.domain.League;
import io.github.davidms.biwenger.app.league.domain.Manager;
import io.github.davidms.biwenger.app.league.domain.Round;
import io.github.davidms.biwenger.app.league.domain.RoundStatus;
import io.github.davidms.biwenger.app.market.domain.MarketListing;
import io.github.davidms.biwenger.app.market.domain.Seller;
import io.github.davidms.biwenger.app.player.domain.Player;
import io.github.davidms.biwenger.app.player.domain.PlayerStatus;
import io.github.davidms.biwenger.app.player.domain.Position;
import io.github.davidms.biwenger.app.player.domain.RoundForm;
import io.github.davidms.biwenger.app.player.domain.Team;
import io.github.davidms.biwenger.client.model.Catalog;
import io.github.davidms.biwenger.client.model.Fitness;
import io.github.davidms.biwenger.client.model.Market;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The mapper against the anonymised samples from docs/biwenger-api/samples/. No Spring, no HTTP.
 */
class BiwengerMapperTest {

  // The client's own setting (BiwengerClient), stated here too so the two can't drift apart.
  private static final JsonMapper JSON =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static Catalog catalog;
  private static Market market;
  private static io.github.davidms.biwenger.client.model.League league;

  @BeforeAll
  static void readSamples() throws IOException {
    catalog = data("catalog.json", Catalog.class);
    market = data("market.json", Market.class);
    league = data("league.json", io.github.davidms.biwenger.client.model.League.class);
  }

  @Test
  void mapsEveryCatalogPlayerInIdOrder() {
    List<Player> players = BiwengerMapper.players(catalog);

    assertThat(players).hasSize(39);
    assertThat(players).extracting(Player::id).isSorted();
  }

  @Test
  void mapsAPlayer() {
    Player ayoze = player(2001);

    assertThat(ayoze.name()).isEqualTo("Ayoze");
    assertThat(ayoze.position()).isEqualTo(Position.FORWARD);
    assertThat(ayoze.price()).isEqualTo(Money.euros(4_780_000));
    assertThat(ayoze.priceChange()).isEqualTo(Money.euros(30_000));
    assertThat(ayoze.status()).isEqualTo(PlayerStatus.AVAILABLE);
    assertThat(ayoze.statusNote()).isEmpty();
    assertThat(ayoze.points()).isEqualTo(35);
    assertThat(ayoze.gamesPlayed()).isEqualTo(9);
    assertThat(ayoze.form())
        .containsExactly(
            new RoundForm.Played(6),
            new RoundForm.Played(2),
            new RoundForm.Played(9),
            new RoundForm.Played(6),
            new RoundForm.Played(0));
  }

  @Test
  void mapsFormWithAbsencesAndRoundsNotPicked() {
    // In the sample: ["sanctioned", -4, null, "doubt", "doubt"].
    assertThat(player(29034).form())
        .containsExactly(
            new RoundForm.Absent(PlayerStatus.SUSPENDED),
            new RoundForm.Played(-4),
            new RoundForm.NotPicked(),
            new RoundForm.Absent(PlayerStatus.DOUBTFUL),
            new RoundForm.Absent(PlayerStatus.DOUBTFUL));
  }

  @Test
  void mapsAnInjuryWithItsNote() {
    Player injured = player(37719);

    assertThat(injured.status()).isEqualTo(PlayerStatus.INJURED);
    assertThat(injured.statusNote()).startsWith("Rotura fibrilar");
  }

  @Test
  void mapsOtherPositions() {
    // A defender allowed in midfield, as in the lineup saved in #19.
    Player bretones = player(33971);

    assertThat(bretones.position()).isEqualTo(Position.DEFENDER);
    assertThat(bretones.canPlay(Position.MIDFIELDER)).isTrue();
  }

  @Test
  void mapsTeamsAndRounds() {
    assertThat(BiwengerMapper.teams(catalog))
        .hasSize(20)
        .contains(new Team(19, "Villarreal", "villarreal"));
    List<Round> rounds = BiwengerMapper.rounds(catalog);
    assertThat(rounds).hasSize(42);
    assertThat(rounds)
        .filteredOn(round -> round.id() == 4937)
        .singleElement()
        .satisfies(
            postponed -> {
              assertThat(postponed.shortName()).isEqualTo("J1");
              assertThat(postponed.part()).isEqualTo(2);
              assertThat(postponed.isPostponed()).isTrue();
              assertThat(postponed.status()).isEqualTo(RoundStatus.FINISHED);
            });
    assertThat(rounds).filteredOn(round -> !round.isPostponed()).allMatch(r -> r.part() == 1);
  }

  @Test
  void mapsTheMarket() {
    List<MarketListing> listings = BiwengerMapper.listings(market);

    assertThat(listings).hasSize(19);
    assertThat(listings).filteredOn(MarketListing::isFreePlayer).hasSize(12);
    MarketListing byManager =
        listings.stream().filter(l -> l.playerId() == 15430).findFirst().orElseThrow();
    assertThat(byManager.seller())
        .isEqualTo(new Seller.ByManager(new Manager(2_000_003, "Manager 3")));
    assertThat(byManager.price()).isEqualTo(Money.euros(6_930_000));
    assertThat(byManager.closesAt()).isEqualTo(Instant.ofEpochSecond(1_792_793_067));
  }

  @Test
  void mapsTheLeagueWithManagersInIdOrder() {
    League mapped = BiwengerMapper.league(league);

    assertThat(mapped.name()).isEqualTo("Example League");
    assertThat(mapped.managers()).hasSize(6).extracting(Manager::id).isSorted();
  }

  @Test
  void translatesBiwengerCodes() {
    assertThat(BiwengerMapper.position(5)).isEqualTo(Position.COACH);
    assertThatThrownBy(() -> BiwengerMapper.position(6))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("6");
    // A status Biwenger adds later must not break the catalog.
    assertThat(BiwengerMapper.status("warned")).isEqualTo(PlayerStatus.UNKNOWN);
    assertThat(BiwengerMapper.roundStatus("cancelled")).isEqualTo(RoundStatus.UNKNOWN);
  }

  @Test
  void aMissedRoundWhileFitIsNotPicked() {
    assertThat(BiwengerMapper.form(new Fitness(null, "ok"))).isEqualTo(new RoundForm.NotPicked());
    assertThat(BiwengerMapper.form(Fitness.NOT_PLAYED)).isEqualTo(new RoundForm.NotPicked());
    assertThat(BiwengerMapper.form(new Fitness(null, "injured")))
        .isEqualTo(new RoundForm.Absent(PlayerStatus.INJURED));
  }

  @Test
  void domainInvariantsHold() {
    assertThatThrownBy(() -> new Round(1, "Jornada 1", "J1", RoundStatus.PENDING, 0))
        .isInstanceOf(IllegalArgumentException.class);
    Instant now = Instant.ofEpochSecond(1_792_745_310);
    assertThatThrownBy(
            () -> new MarketListing(1, new Seller.Game(), Money.ZERO, now, now.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Player player(long id) {
    return BiwengerMapper.players(catalog).stream()
        .filter(p -> p.id() == id)
        .findFirst()
        .orElseThrow();
  }

  private static <T> T data(String sample, Class<T> type) throws IOException {
    try (InputStream in = BiwengerMapperTest.class.getResourceAsStream("/samples/" + sample)) {
      assertThat(in).as("sample %s on the test classpath", sample).isNotNull();
      return JSON.treeToValue(JSON.readTree(in).get("data"), type);
    }
  }
}
