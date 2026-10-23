package io.github.davidms.biwenger.app.account.domain;

import java.util.Objects;

/** The Biwenger account the app works for: who logs in, their team, and the league it plays. */
public record ConnectedAccount(String accountName, String teamName, String leagueName) {

  public ConnectedAccount {
    Objects.requireNonNull(accountName, "accountName");
    Objects.requireNonNull(teamName, "teamName");
    Objects.requireNonNull(leagueName, "leagueName");
  }
}
