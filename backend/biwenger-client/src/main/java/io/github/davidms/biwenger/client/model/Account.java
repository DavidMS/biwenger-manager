package io.github.davidms.biwenger.client.model;

import java.util.List;
import java.util.Objects;

/** {@code GET /api/v2/account}: the account and the leagues it plays. */
public record Account(AccountProfile account, List<AccountLeague> leagues) {

  public Account {
    Objects.requireNonNull(account, "account");
    leagues = leagues == null ? List.of() : List.copyOf(leagues);
  }
}
