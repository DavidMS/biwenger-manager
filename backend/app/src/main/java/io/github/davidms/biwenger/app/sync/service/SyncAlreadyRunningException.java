package io.github.davidms.biwenger.app.sync.service;

/** A sync was asked for while another one runs. Only one runs at a time. */
public class SyncAlreadyRunningException extends RuntimeException {

  public SyncAlreadyRunningException() {
    super("A sync is already running");
  }
}
