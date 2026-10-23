# Write endpoints

The requests that **change the account**: bid, cancel a bid, list a player for sale, withdraw a listing, save the lineup. They were captured once, in #19, from the browser while the human did each action on biwenger.as.com. **Nothing here was sent by Claude or by code.**

> **Golden rule 2.** The app sends these only through the propose → confirm flow (Phase 6, #42–#44), after the user has confirmed. The client never retries them (#18): only `GET`s are retried, because a bid sent twice is a real loss.

## Common to every write

| Header | Value | Note |
|---|---|---|
| `Authorization` | `Bearer <token>` | Removed from the samples |
| `X-League` / `X-User` | league id / your user id in it | As for reads |
| `X-Lang` | `es` | |
| `Content-Type` | `application/json; charset=utf-8` | Also sent with a `DELETE` that has no body |
| `Accept` | `application/json` | |
| `X-Version` | `631` | Sent by the web app on every call. It equals the account's `data.version` (`631` in the [account sample](samples/account.json)). Whether the server requires it is unknown: send it |

Answers: a write either returns the usual `{status, data}` envelope with `200`, or **`204` with no body**. The server's error answers (bad amount, deadline passed) were not captured.

## Place a bid

`POST /api/v2/offers`

```json
{ "to": null, "type": "purchase", "amount": 2560000, "requestedPlayers": [19860] }
```

| Field | Meaning |
|---|---|
| `type` | `"purchase"`: we want to buy |
| `to` | `null` for a free player sold by the game. For a manager's player it is probably their user id (inferred: not captured) |
| `amount` | The bid, whole euros |
| `requestedPlayers` | The player ids, as an array. One player here |

Answer `200`:

```json
{ "status": 200, "data": { "id": 9000001, "type": "purchase", "status": "waiting",
  "fromID": 2000001, "toID": 0, "amount": 2560000, "created": 1792745310, "until": 1792818000 } }
```

- **A bid is an offer.** It gets an offer `id` (needed to cancel it), `status: "waiting"`, and `toID: 0` for the game.
- **`until` is the market close:** `1792818000` is 2026-10-24 05:00 UTC (07:00 in Madrid), the same as the sale's `until` in the market. Bids are decided then.
- The amount bid here was the player's asking price. Bids below it, and above `maximumBid` (balance + 25 % of team value, see [market.md](market.md)), were not tried, so the server's rules for them are unknown.

Sample: [01-post-offers.json](samples/write/01-post-offers.json)

## Cancel a bid

`DELETE /api/v2/offers/{offerId}` with no body. Answer **`204`**, no body.

`offerId` is the `id` from the bid's answer. Sample: [02-delete-offers-9000001.json](samples/write/02-delete-offers-9000001.json)

## List a player for sale

`POST /api/v2/market`

```json
{ "type": "sell", "player": 2001, "price": 14820000 }
```

Answer **`204`**, no body. The player appears in the market for 48 hours (see [market.md](market.md)). Sample: [03-post-market.json](samples/write/03-post-market.json)

## Withdraw a listing

`DELETE /api/v2/market?player={playerId}` with no body. Answer **`204`**, no body.

The player goes in the query string, not the path. pybiwenger's `url_retire_market` (`/market?player=`) matches. Sample: [04-delete-market.json](samples/write/04-delete-market.json)

## Save the lineup

`PUT /api/v2/user?fields=*,lineup(date)` (the comma is sent encoded as `%2C`)

```json
{ "lineup": { "type": "3-4-3",
  "playersID": [30487, 35156, 23572, 26344, null, 33971, 41090, 33697, null, 2001, 2602],
  "reservesID": [41268, null, 41411, null] } }
```

| Field | Meaning |
|---|---|
| `type` | Formation, defenders-midfielders-forwards |
| `playersID` | **Always 11 slots, in formation order:** the goalkeeper, then the formation's defender, midfielder and forward slots (for `3-4-3`: slots 2–4, 5–8, 9–11). A player can sit in a slot of one of his `altPositions`: here a defender with `altPositions: [3]` fills a midfield slot, and a midfielder with `[4]` a forward slot. `null` is an empty slot; the web app saved two empty slots without complaint |
| `reservesID` | **Always 4 slots**, `null` for empty. They look like one per position: a goalkeeper in the first, a midfielder in the third (inferred). As in [rounds-league](rounds-league.md) |

Answer `200`: your user in the league (`id`, `name`, `points`, `balance`, `lastAccess`, `credits`) and `lineup.date`, the time it was saved.

- **The whole lineup is sent every time**, not a change. The two saves in #19 differ in one slot (`26344` ↔ `34181`).
- **No captain or coach in the body.** The account's league has `lineupCaptain` and `lineupCoach` enabled, but this lineup had neither, as [rounds-league](rounds-league.md) shows. The field names for them are unknown: capture a save with a captain before implementing it.
- One of the saved players (`34181`) is not in the catalog (he left La Liga). The server accepted him.

Samples: [05-put-user.json](samples/write/05-put-user.json), [06-put-user.json](samples/write/06-put-user.json)

## Not captured yet

| Action | Why | What to capture |
|---|---|---|
| Accept or reject an offer | The human chose not to: accepting sells the player, and no offer was waiting to reject | A rejection of an offer the human would reject anyway |
| Bid on a manager's player, and offers to managers | Not in scope of the capture | One offer to a manager, then cancel it |
| Captain and coach in the lineup | The lineup had neither | One save with a captain |
| Error answers | All six actions succeeded | A bid below the asking price; a lineup save after the round's deadline |

## Rules the server enforces

What we know, and what we don't:

| Rule | Status | Source |
|---|---|---|
| A bid is decided when the market closes, at 05:00 UTC | Known | The bid's `until` equals the market's |
| A bid can exceed the balance, up to `maximumBid` = balance + 25 % of team value | Inferred | [market.md](market.md); not tested against the server |
| Minimum bid (the asking price?) | **Unknown** | Not tried |
| A listing lasts 48 hours | Inferred | [market.md](market.md) |
| A lineup needs 11 slots and 4 reserves, `null` for empty | Known for the format; validation unknown | The two saves |
| Lineup changes close when the round starts | **Unknown** | Not tried. The league has `lineupRoundChanges` settings whose meaning is unconfirmed |

Phase 6 must validate these itself in *propose*, before anything reaches the server, and show the server's answer when it refuses.

## How the samples were captured

1. The human recorded the Network tab in Chrome DevTools, with *Preserve log* on, reloaded biwenger.as.com and did each action once. Then they exported the HAR to a folder outside the repo.
2. The human ran `python3 scripts/biwenger_har_extract.py <har files>`. It keeps only non-`GET` API requests and skips the login. It removes `Authorization`, cookies and other headers (listed by name only). It anonymises with the account call in the HAR (you are Manager 1, league `1000001`, as in the read samples) and leak-checks every file, including for personal avatar URLs.
3. Claude read only the output, reviewed each field by hand, and replaced the real offer id with `9000001`.
4. The human deleted the HAR files, which hold the session.
