package io.github.davidms.biwenger.app.account.web;

import io.github.davidms.biwenger.app.account.domain.ConnectedAccount;

/** {@code GET /api/account}: the connected Biwenger account. */
public record AccountResponse(String accountName, String teamName, String leagueName) {

  static AccountResponse from(ConnectedAccount account) {
    return new AccountResponse(account.accountName(), account.teamName(), account.leagueName());
  }
}
