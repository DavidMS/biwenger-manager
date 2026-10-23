package io.github.davidms.biwenger.app.sync.persistence;

/** Lets tests in other packages reach {@link SyncRunEntity}'s package-private helpers. */
public final class SyncRunEntityAccess {

  private SyncRunEntityAccess() {}

  public static String truncate(String text) {
    return SyncRunEntity.truncate(text);
  }
}
