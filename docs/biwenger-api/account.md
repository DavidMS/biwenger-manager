# Account

Returns the logged-in account and its leagues, with your team in each league (balance, points, position) and the league settings. Used at startup to pick the single La Liga league and its `X-League` / `X-User` values, and by the **dashboard** (F1: balance, position, negative-balance warning) and the **advisor** (lineup and clause rules).

## Request

`GET https://biwenger.as.com/api/v2/account`

Headers sent: `Authorization: Bearer <token>`, `Accept: application/json`, `X-Lang: es`, `User-Agent`. **Not** `X-League` / `X-User`: they come from this response. No query parameters.

## Response

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.account.id` | int | Account id. **Not** the `X-User` value | `3000001` |
| `data.account.name` | string | Account display name | `"Manager 1"` |
| `data.leagues[]` | array | One entry per league the account plays | |
| `data.leagues[].id` | int | League id. Send as `X-League` | `1000001` |
| `data.leagues[].name` | string | League name | `"Example League"` |
| `data.leagues[].competition` | string | Competition slug. Pick the league with `"la-liga"` | `"la-liga"` |
| `data.leagues[].scoreID` | int | Scoring system. Pass as `score=` to the catalog | `1` (Diario AS) |
| `data.leagues[].type` / `mode` / `marketMode` | string | League kind | `"premium"`, `"league"`, `"normal"` |
| `data.leagues[].user.id` | int | Your user id in this league. Send as `X-User` | `2000001` |
| `data.leagues[].user.balance` | int (€) | Your cash. Negative at round start means 0 points | `51420000` |
| `data.leagues[].user.points` | int | Your total points this season | `220` |
| `data.leagues[].user.position` | int | Your position in the standings (1 = first) | `6` |
| `data.leagues[].user.status.offers` / `.bids` | int | Counts of open offers and bids (inferred) | `0` |
| `data.leagues[].settings.clause` | string | Clause mode. `"steal"` seems to mean clauses can be paid to take a player (inferred) | `"steal"` |
| `data.leagues[].settings.clauseIncrement` | int | Clause increment setting, unit unknown | `0` |
| `data.leagues[].settings.balance` | string | `"hidden"`: rivals' balances are not shown (inferred) | `"hidden"` |
| `data.leagues[].settings.marketShowBids` | bool | Whether rivals' bids are visible | `false` |
| `data.leagues[].settings.userOffers` | string | When managers can send offers | `"always"` |
| `data.leagues[].settings.lineupCaptain` / `lineupCoach` / `lineupReserves` / `lineupMultiPos` | bool | Lineup rules | `true` |
| `data.leagues[].settings.lineupMaxClubPlayers` | int | Max players per club. `0` means no limit (inferred) | `0` |
| `data.leagues[].settings.lineupStriker` / `lineupAllowExtra` | bool | Other lineup rules, meaning not confirmed | `false` / `true` |
| `data.leagues[].settings.maxPurchasePrice` | int (€) | Purchase price cap. `0` means none (inferred) | `0` |

Other fields (`upgrades`, `notifications`, `version`, `dailyStreak`, `credits`) are not needed.

## Notes / quirks

- Three different ids: account (`3000001`), league (`1000001`) and your user in the league (`2000001`). `X-User` is the league user id.
- `settings.balance: "hidden"` matters for F4: rival balances must be estimated from the board, not read.
- `settings.clause` is a string mode, not a boolean. Only `"steal"` has been seen.
- The account does not include your team value. It is in [rounds-league](rounds-league.md) (`standings[].teamValue`).
- The account list may hold leagues of other competitions. Filter by `competition == "la-liga"` and fail clearly unless exactly one remains (the script does this).

## Sample

[samples/account.json](samples/account.json)

Anonymised: account id and name, league id and name, your league user id and name, `icon` / `cover` URLs, `email`, `phone`, `birthday`, `devices`, `settings.secret` (the league invite secret) and the top-level `location` object.
