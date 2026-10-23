package io.github.davidms.biwenger.app.sync.service;

import io.github.davidms.biwenger.app.biwenger.BiwengerGateway;
import io.github.davidms.biwenger.app.biwenger.DailyData;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Takes today's snapshot: reads the catalog, market and standings from Biwenger, then stores them
 * for today's Madrid date. Every Biwenger call happens before the database transaction starts, so
 * no transaction waits on the network.
 *
 * <p>The scheduler runs it on one thread, so it never overlaps with itself. The manual trigger
 * (#25) must guard against overlap here, in the service, so the scheduled run respects it too.
 */
@Service
public class DailySyncService {

  private static final System.Logger LOG = System.getLogger(DailySyncService.class.getName());

  private final BiwengerGateway biwenger;
  private final SnapshotWriter writer;
  private final Clock clock;

  DailySyncService(BiwengerGateway biwenger, SnapshotWriter writer, Clock clock) {
    this.biwenger = biwenger;
    this.writer = writer;
    this.clock = clock;
  }

  public SyncSummary syncToday() {
    Instant started = clock.instant();
    LocalDate day = LocalDate.ofInstant(started, clock.getZone());
    DailyData data = biwenger.dailyData();
    writer.write(day, started, data);
    var summary =
        new SyncSummary(
            day,
            data.players().size(),
            data.market().size(),
            data.standings().size(),
            Duration.between(started, clock.instant()));
    LOG.log(Level.INFO, "Daily sync done, {0}", summary);
    return summary;
  }
}
