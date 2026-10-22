# Market

Returns today's market: free players and players listed by managers, with asking price and closing time, plus your balance and maximum bid. Used by the **market view** (F3), **buy recommendations** (F7), the **bid advisor** (F8) and the **dashboard** (F1).

## Request

`GET https://biwenger.as.com/api/v2/market`

Headers sent: `Authorization: Bearer <token>`, `X-League: {leagueId}`, `X-User: {userId}`, `X-Lang: es`, `Accept: application/json`, `User-Agent`. No query parameters.

## Response

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.status.balance` | int (€) | Your cash (same as the account's `user.balance`) | `51420000` |
| `data.status.maximumBid` | int (€) | The highest bid you may place now | `57005000` |
| `data.sales[]` | array | Players on sale (19 in the sample) | |
| `data.sales[].player.id` | int | Player id. Join with the catalog | `16738` |
| `data.sales[].price` | int (€) | Asking price | `5640000` |
| `data.sales[].date` | int (Unix s) | When the player was listed | `1792645525` |
| `data.sales[].until` | int (Unix s) | When the sale closes | `1792731600` |
| `data.sales[].extended` | bool, optional | Present and `true` on free players listed the day before (inferred: unsold and kept for another day) | `true` |
| `data.sales[].user` | object or `null` | The seller. **`null` means a free player** sold by the game | |
| `data.sales[].user.id` / `.name` | int / string | Selling manager | `2000003`, `"Manager 3"` |
| `data.sales[].player.owner.price` | int (€) | What the seller paid (inferred). Only on managers' sales | `10610820` |
| `data.sales[].player.owner.clause` | int (€) | Current release clause | `21221640` |
| `data.sales[].player.owner.date` | int (Unix s) | When the seller got the player | `1786856548` |
| `data.sales[].player.owner.clauseLockedUntil` | int (Unix s), optional | Until when the clause can't be paid | `1793941548` |
| `data.offers[]` | array | Offers received or sent (empty in the sample, shape unknown) | `[]` |

## Notes / quirks

- `player` is an object, not a bare id. Free players have only `{id}`; managers' players add `owner`.
- **Maximum bid** (inferred from one data point): `maximumBid = balance + 25 % of your team value`. Here `51420000 + 0.25 × 22340000 = 57005000` exactly (team value from rounds-league). So a winning bid can push the balance negative: the bid advisor must warn.
- **Closing times.** All free players close at the same time (`1792731600`, 05:00 UTC, 07:00 in Madrid). New free players were listed at about 05:05 UTC the day before. Managers' sales close exactly 48 h after `date`.
- **Clauses.** For most managers' sales `clause = 2 × owner.price`. One player had a higher clause, so owners can probably raise it (inferred). `clauseLockedUntil` was exactly 21 days after `owner.date` when present (inferred: clause protection after a purchase).
- A free player's `price` can differ slightly from the catalog `price`: two extended sales show 10 000 € more than the catalog (yesterday's value, inferred). Others match exactly. Show the market `price` as the asking price.
- Managers can ask less than they paid: one sale lists at 6.93 M€ a player bought for 10.61 M€.
- Bids are not visible (`settings.marketShowBids: false` in the account). Your own bids may appear here when you have some; not captured yet.

## Sample

[samples/market.json](samples/market.json)

Anonymised: selling managers' ids, names and icons (`sales[].user`). Player ids, prices and dates are public and unchanged.
