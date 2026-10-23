package io.github.davidms.biwenger.app.sync.persistence;

import io.github.davidms.biwenger.app.sync.domain.SyncResult;
import io.github.davidms.biwenger.app.sync.domain.SyncRun;
import io.github.davidms.biwenger.app.sync.domain.SyncTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** One row of {@code sync_run}. */
@Entity
@Table(name = "sync_run")
public class SyncRunEntity {

  /** Long enough for any error message we write; the rest is cut. */
  static final int MAX_ERROR_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "sync_trigger", nullable = false)
  private SyncTrigger trigger;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SyncResult result;

  @Column(name = "snapshot_date")
  private LocalDate snapshotDate;

  private Integer players;

  @Column(name = "market_listings")
  private Integer marketListings;

  private Integer standings;

  private String error;

  protected SyncRunEntity() {}

  public static SyncRunEntity started(SyncTrigger trigger, Instant startedAt) {
    var entity = new SyncRunEntity();
    entity.trigger = trigger;
    entity.startedAt = startedAt;
    entity.result = SyncResult.RUNNING;
    return entity;
  }

  public void succeeded(
      Instant finishedAt, LocalDate day, int players, int marketListings, int standings) {
    this.finishedAt = finishedAt;
    this.result = SyncResult.SUCCEEDED;
    this.snapshotDate = day;
    this.players = players;
    this.marketListings = marketListings;
    this.standings = standings;
    this.error = null;
  }

  public void failed(Instant finishedAt, String error) {
    this.finishedAt = finishedAt;
    this.result = SyncResult.FAILED;
    String reason = error == null || error.isBlank() ? "unknown error" : error;
    this.error = truncate(reason);
  }

  /** Cut to {@link #MAX_ERROR_LENGTH} characters, never in the middle of one (surrogate pairs). */
  static String truncate(String text) {
    if (text.length() <= MAX_ERROR_LENGTH) {
      return text;
    }
    int end = MAX_ERROR_LENGTH;
    if (Character.isHighSurrogate(text.charAt(end - 1))) {
      end--;
    }
    return text.substring(0, end);
  }

  public Long id() {
    return id;
  }

  public SyncRun toDomain() {
    return switch (result) {
      case RUNNING -> new SyncRun.Running(trigger, startedAt);
      case SUCCEEDED ->
          new SyncRun.Succeeded(
              trigger, startedAt, finishedAt, snapshotDate, players, marketListings, standings);
      case FAILED -> new SyncRun.Failed(trigger, startedAt, finishedAt, error);
    };
  }
}
