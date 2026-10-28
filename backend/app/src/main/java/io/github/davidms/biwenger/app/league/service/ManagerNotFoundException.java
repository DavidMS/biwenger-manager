package io.github.davidms.biwenger.app.league.service;

/** No manager with this id is in the league. */
public class ManagerNotFoundException extends RuntimeException {

  public ManagerNotFoundException(long managerId) {
    super("No manager " + managerId + " in the league");
  }
}
