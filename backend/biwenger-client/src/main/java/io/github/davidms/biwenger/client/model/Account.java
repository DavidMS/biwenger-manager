package io.github.davidms.biwenger.client.model;

import java.util.List;

/** {@code GET /api/v2/account}: the account and the leagues it plays. */
public record Account(AccountProfile account, List<AccountLeague> leagues) {

  public Account {
    leagues = leagues == null ? List.of() : List.copyOf(leagues);
  }
}
