package io.github.davidms.biwenger.app.account.web;

import io.github.davidms.biwenger.app.account.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Which Biwenger account and league the app is connected to. */
@RestController
@RequestMapping("/api/account")
class AccountController {

  private final AccountService accounts;

  AccountController(AccountService accounts) {
    this.accounts = accounts;
  }

  /** {@code 503} when no login is set or Biwenger rejects it; {@code 502} when Biwenger fails. */
  @GetMapping
  AccountResponse account() {
    return AccountResponse.from(accounts.connectedAccount());
  }
}
