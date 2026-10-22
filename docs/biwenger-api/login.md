# Login

Exchanges the user's email and password for a bearer token. Every other `biwenger.as.com` call needs it. Used by the client's auth layer (issue 2.2), not by a feature directly.

## Request

`POST https://biwenger.as.com/api/v2/auth/login`

| Header | Value |
|---|---|
| `Content-Type` | `application/json` |
| `Accept` | `application/json` |
| `X-Lang` | `es` |
| `User-Agent` | identifying string |

No `Authorization`, `X-League` or `X-User` (we don't have them yet).

Body:

```json
{ "email": "<BIWENGER_EMAIL>", "password": "<BIWENGER_PASSWORD>" }
```

Both values come from environment variables (`.env`, never committed).

## Response

```json
{ "token": "<opaque string>" }
```

| JSON path | Type | Meaning |
|---|---|---|
| `token` | string | Bearer token. Send it as `Authorization: Bearer <token>` |

- There is **no** `{status, data}` envelope here: the script reads `token` at the top level and it worked.
- The observed body was 137 bytes, which fits a single `token` field and nothing else (inferred).
- Token lifetime is unknown. The client should log in again once on `401` and fail on a second `401`.

## Notes / quirks

- The login is the only request in the capture with a body and the only `POST`.
- The script printed only the status code and size of this response, never the body.
- Error responses for bad credentials were not captured. The script treats "no `token` field" as a failed login. WireMock tests for bad credentials should use a plain `401` until we capture the real body.

## Sample

None. The response only holds a token, so there is nothing safe or useful to save. Tests can stub it as `{"token": "test-token"}`.
