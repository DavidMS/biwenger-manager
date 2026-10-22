# Board (transfers)

Returns the league's recent transfer feed: market purchases (with losing bids), sales to the game and clause buys. Used by **standings and rivals** (F4: estimating rival balances, which the league hides), the **bid advisor** (F8: what rivals bid), **clause watch** (F13) and **season analytics** (F15).

## Request

`GET https://biwenger.as.com/api/v2/league/{leagueId}/board?type=transfer,market`

Headers sent: `Authorization: Bearer <token>`, `X-League: {leagueId}`, `X-User: {userId}`, `X-Lang: es`, `Accept: application/json`, `User-Agent`.

| Query parameter | Value | Meaning |
|---|---|---|
| `type` | `transfer,market` | Comma-separated entry types. Other types (posts, for example) probably exist; not tried |

## Response

`data` is an **array** of entries, newest first.

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data[].type` | string | `"market"` or `"transfer"` | `"market"` |
| `data[].date` | int (Unix s) | When it happened | `1792645526` |
| `data[].content[]` | array | One item per player moved | |
| `data[].content[].player` | int | Player id (a bare id, not an object) | `22964` |
| `data[].content[].amount` | int (€) | Price paid or received | `3318750` |
| `data[].content[].to` | object, optional | Buying manager: `id`, `name`, `icon` | `"Manager 3"` |
| `data[].content[].from` | object, optional | Selling manager | `"Manager 4"` |
| `data[].content[].type` | string, optional | `"clause"` for a clause buy | `"clause"` |
| `data[].content[].bids[]` | array, optional | Losing bids: `user` (manager) and `amount` | `[{user, 3145000}]` |
| `data[].title` / `fixed` / `author` | string / bool / null | Not used (empty in all entries) | |

How to read an item (inferred from the data):

| Entry `type` | Has `from` | Has `to` | Meaning |
|---|---|---|---|
| `market` | no | yes | `to` won a market auction for `amount`. `bids` lists the other bids |
| `transfer` | yes | no | `from` sold the player to the game for `amount` |
| `transfer` + item `type: "clause"` | yes | yes | `to` paid `from`'s clause |

## Notes / quirks

- The board returned **10 entries**, and the script keeps up to 20, so the sample is complete (10 of 10). The server probably pages the feed; how to get older entries is unknown.
- Market purchases happen at the market close (about 05:05 UTC). One entry can hold several players.
- The losing bids are visible here even though the market hides bids. This is the data the bid advisor needs.
- Amounts are odd numbers like `3318750`: bids are not rounded.
- A sale between two managers (an accepted offer) was not in this sample.

## Sample

[samples/board.json](samples/board.json)

Anonymised: manager ids, names and icons in `from`, `to` and `bids[].user`. Kept 10 of 10 entries.
