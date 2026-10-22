#!/usr/bin/env python3
"""Capture anonymised samples of the Biwenger read endpoints (issue #14).

Run it yourself from the repo root: python3 scripts/biwenger_capture.py

- Reads BIWENGER_EMAIL and BIWENGER_PASSWORD from .env (or the environment).
- Logs in once, then makes GET requests only, two seconds apart. Nothing in Biwenger changes.
- Anonymises every response in memory. Raw responses and the token are never written to disk.
- Before writing a file, checks it for the email, password, token, real manager and league names
  and ids, and the league's invite secret. A file that fails the check is not written.
- Prints endpoint names, HTTP status codes and sizes only: no URLs (they hold the league id) and no
  response bodies.

Samples go to docs/biwenger-api/samples/. Standard library only.
"""

import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

API = "https://biwenger.as.com/api/v2"
CDN = "https://cf.biwenger.com/api/v2"
USER_AGENT = "biwenger-manager/0.1 (personal capture script; +https://github.com/DavidMS/biwenger-manager)"
PAUSE_SECONDS = 2
REPORT_FIELDS = "*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away))"

# Keys whose values are personal or secret, wherever they appear. Matched case-insensitively.
DROP_KEYS = {"email", "phone", "birthday", "devices", "token", "password", "secret", "location",
             "ip", "facebook", "twitter", "instagram", "google_id", "apple_id"}
# Of those, the ones whose values are also searched for in the output. Not location or birthday:
# a city like "Madrid" also appears in public football data ("Real Madrid").
LEAK_CHECKED_KEYS = DROP_KEYS - {"location", "birthday"}
# Keys that make a {id, name} object a manager rather than a player, team or round.
MANAGER_HINTS = {"icon", "balance", "teamValue", "joinDate", "role"}
CATALOG_EXTRA_PLAYERS = 10
CATALOG_MAX_PLAYERS = 40
BOARD_MAX_ENTRIES = 20


def placeholder(value):
    """A value of the same JSON type, so the samples still show each field's type."""
    if isinstance(value, bool):
        return False
    if isinstance(value, (int, float)):
        return 0
    if isinstance(value, str):
        return "removed"
    if isinstance(value, list):
        return []
    if isinstance(value, dict):
        return {}
    return None


class Anonymiser:
    """Replaces managers, leagues and the account with consistent fake names and ids."""

    def __init__(self):
        self.ids = {}  # real id -> fake id
        self.names = {}  # real name -> fake name
        self.secrets = {}  # value of a dropped key -> that key, checked for leaks
        self.objects = set()  # id() of dicts that are a manager, league or the account

    def add_account(self, account):
        self._add(account, "Account Owner", 3_000_001)

    def add_league(self, league, n):
        self._add(league, "Example League" if n == 1 else f"Other League {n}", 1_000_000 + n)

    def add_manager(self, manager):
        real_id = manager.get("id")
        if real_id in self.ids:
            self.objects.add(id(manager))
            return
        n = 1 + sum(1 for fake in self.ids.values() if 2_000_000 < fake < 3_000_000)
        self._add(manager, f"Manager {n}", 2_000_000 + n)

    def _add(self, obj, fake_name, fake_id):
        self.objects.add(id(obj))
        if isinstance(obj.get("id"), int) and obj["id"] not in self.ids:
            self.ids[obj["id"]] = fake_id
        if isinstance(obj.get("name"), str) and obj["name"] not in self.names:
            self.names[obj["name"]] = fake_name

    def find_managers(self, value):
        """Registers every {id, name} object that looks like a manager (not a player or team)."""
        if isinstance(value, dict):
            if (isinstance(value.get("id"), int) and isinstance(value.get("name"), str)
                    and "slug" not in value and MANAGER_HINTS & value.keys()):
                self.add_manager(value)
            for child in value.values():
                self.find_managers(child)
        elif isinstance(value, list):
            for child in value:
                self.find_managers(child)

    def collect_secrets(self, value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key.lower() in LEAK_CHECKED_KEYS:
                    self._collect_strings(child, key)
                else:
                    self.collect_secrets(child)
        elif isinstance(value, list):
            for child in value:
                self.collect_secrets(child)

    def _collect_strings(self, value, key):
        if isinstance(value, str) and len(value) >= 4:
            self.secrets.setdefault(value, key)
        elif isinstance(value, dict):
            for child in value.values():
                self._collect_strings(child, key)
        elif isinstance(value, list):
            for child in value:
                self._collect_strings(child, key)

    def apply(self, value):
        if isinstance(value, dict):
            out = {}
            for key, child in value.items():
                new_key = self._text(key)
                if key.lower() in DROP_KEYS:
                    out[new_key] = placeholder(child)
                elif id(value) in self.objects and key in ("icon", "cover") and isinstance(child, str):
                    out[new_key] = f"https://example.invalid/{key}.png"
                else:
                    out[new_key] = self.apply(child)
            return out
        if isinstance(value, list):
            return [self.apply(child) for child in value]
        if isinstance(value, bool):
            return value
        if isinstance(value, int):
            return self.ids.get(value, value)
        if isinstance(value, str):
            return self._text(value)
        return value

    def _text(self, text):
        if text in self.names:
            return self.names[text]
        for real in sorted(self.names, key=len, reverse=True):
            if len(real) >= 3:
                text = re.sub(re.escape(real), self.names[real], text, flags=re.IGNORECASE)
        for real, fake in self.ids.items():
            text = re.sub(rf"(?<!\d){real}(?!\d)", str(fake), text)
        return text

    def leaks(self, text, extra=()):
        """Names the kinds of real data still present in text. Never returns the values."""
        found = []
        lowered = text.lower()
        if any(len(name) >= 3 and name.lower() in lowered for name in self.names):
            found.append("manager or league name")
        if any(re.search(rf"(?<!\d){real}(?!\d)", text) for real in self.ids):
            found.append("manager, league or account id")
        keys = sorted({key for secret, key in self.secrets.items() if secret in text})
        if keys:
            # The key a value came from is safe to show; the value is not.
            found.append(f"a value of a removed field ({', '.join(keys)})")
        if any(value and len(value) >= 4 and value.lower() in lowered for value in extra):
            found.append("email, password or token")
        return found


def trim_catalog(catalog, keep_ids):
    """Keeps the given players plus a few more, so the sample stays small but realistic."""
    players = catalog.get("data", {}).get("players")
    if not isinstance(players, dict):
        return catalog, 0
    by_id = sorted(players, key=lambda key: int(key) if key.isdigit() else 0)
    wanted = [key for key in by_id if key in {str(k) for k in keep_ids}]
    wanted += [key for key in by_id if key not in wanted][:CATALOG_EXTRA_PLAYERS]
    catalog["data"]["players"] = {key: players[key] for key in wanted[:CATALOG_MAX_PLAYERS]}
    return catalog, len(players)


def load_credentials(root):
    values = {}
    env_file = root / ".env"
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            key, sep, value = line.strip().partition("=")
            if sep and not key.startswith("#"):
                values[key.strip()] = value.strip().strip("'\"")
    email = os.environ.get("BIWENGER_EMAIL") or values.get("BIWENGER_EMAIL")
    password = os.environ.get("BIWENGER_PASSWORD") or values.get("BIWENGER_PASSWORD")
    if not email or not password:
        sys.exit("Set BIWENGER_EMAIL and BIWENGER_PASSWORD in .env (see .env.example).")
    return email, password


class Biwenger:
    def __init__(self):
        self.headers = {"Accept": "application/json", "X-Lang": "es", "User-Agent": USER_AGENT}
        self.calls = 0

    def request(self, name, url, body=None):
        if self.calls:
            time.sleep(PAUSE_SECONDS)
        self.calls += 1
        data = json.dumps(body).encode() if body is not None else None
        headers = dict(self.headers, **({"Content-Type": "application/json"} if data else {}))
        req = urllib.request.Request(url, data=data, headers=headers, method="POST" if data else "GET")
        try:
            with urllib.request.urlopen(req, timeout=20) as response:
                payload = response.read()
                print(f"  {name:<16} HTTP {response.status}  {len(payload):>9,} bytes")
                return json.loads(payload)
        except urllib.error.HTTPError as error:
            print(f"  {name:<16} HTTP {error.code}  (body not shown)")
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
            print(f"  {name:<16} failed: {type(error).__name__}")
        return None


def main(root=None, out_dir=None):
    root = root or Path(__file__).resolve().parent.parent
    out_dir = out_dir or root / "docs" / "biwenger-api" / "samples"
    email, password = load_credentials(root)
    api = Biwenger()

    print("Requests (one login, then GET only):")
    login = api.request("login", f"{API}/auth/login", {"email": email, "password": password})
    token = (login or {}).get("token")
    if not token:
        sys.exit("Login failed: check the credentials in .env.")
    api.headers["Authorization"] = f"Bearer {token}"

    account = api.request("account", f"{API}/account")
    leagues = ((account or {}).get("data") or {}).get("leagues") or []
    la_liga = [league for league in leagues if league.get("competition") == "la-liga"]
    if len(la_liga) != 1:
        sys.exit(f"Expected exactly one La Liga league, found {len(la_liga)}. Nothing written.")
    league = la_liga[0]
    api.headers["X-League"] = str(league["id"])
    api.headers["X-User"] = str(league["user"]["id"])

    responses = {"account": account}
    responses["league"] = api.request("league", f"{API}/league/{league['id']}")
    responses["user-squad"] = api.request("user-squad", f"{API}/user?fields=players(id,owner)")
    responses["market"] = api.request("market", f"{API}/market")
    responses["catalog"] = api.request(
        "catalog", f"{API}/competitions/la-liga/data?lang=es&score={league.get('scoreID', 1)}")
    responses["rounds-league"] = api.request("rounds-league", f"{API}/rounds/league")
    responses["board"] = api.request("board", f"{API}/league/{league['id']}/board?type=transfer,market")

    squad_ids = [p.get("id") for p in ((responses["user-squad"] or {}).get("data") or {}).get("players", [])]
    market_ids = [(s.get("player") or {}).get("id") if isinstance(s.get("player"), dict) else s.get("player")
                  for s in ((responses["market"] or {}).get("data") or {}).get("sales", [])]
    catalog_players = ((responses["catalog"] or {}).get("data") or {}).get("players") or {}
    slug = next((catalog_players[str(i)].get("slug") for i in squad_ids if str(i) in catalog_players), None)
    if slug:
        # Public CDN: no Authorization header needed or sent.
        cdn = Biwenger()
        cdn.calls = 1
        responses["player-prices"] = cdn.request("player-prices", f"{CDN}/players/la-liga/{slug}?lang=es&fields=*,prices")
        responses["player-reports"] = cdn.request("player-reports", f"{CDN}/players/la-liga/{slug}?lang=es&fields={REPORT_FIELDS}")

    anonymiser = Anonymiser()
    anonymiser.add_account((account.get("data") or {}).get("account") or {})
    anonymiser.add_manager(league["user"])  # you are always Manager 1
    for n, other in enumerate([league] + [lg for lg in leagues if lg is not league], start=1):
        anonymiser.add_league(other, n)
    for response in responses.values():
        anonymiser.find_managers(response)
        anonymiser.collect_secrets(response)

    collisions = set(anonymiser.ids) & {int(k) for k in catalog_players if k.isdigit()}
    if collisions:
        print(f"Warning: {len(collisions)} manager or league id(s) equal a player id; check the samples.")

    print("\nSamples:")
    out_dir.mkdir(parents=True, exist_ok=True)
    failed = 0
    for name, response in responses.items():
        if response is None:
            print(f"  {name:<16} skipped (request failed)")
            continue
        note = ""
        if name == "catalog":
            response, total = trim_catalog(response, squad_ids + market_ids)
            note = f"  (kept {len(response['data']['players'])} of {total} players)"
        if name == "board" and isinstance(response.get("data"), list):
            note = f"  (kept {min(len(response['data']), BOARD_MAX_ENTRIES)} of {len(response['data'])} entries)"
            response["data"] = response["data"][:BOARD_MAX_ENTRIES]
        text = json.dumps(anonymiser.apply(response), indent=2, ensure_ascii=False) + "\n"
        leaks = anonymiser.leaks(text, extra=(email, password, token))
        if leaks:
            failed += 1
            print(f"  {name:<16} NOT written: still contains {', '.join(leaks)}")
            continue
        (out_dir / f"{name}.json").write_text(text)
        print(f"  {name:<16} written, {len(text):>9,} bytes{note}")

    print(f"\n{api.calls + (2 if slug else 0)} requests. "
          f"{len(anonymiser.ids)} ids and {len(anonymiser.names)} names replaced. {failed} file(s) not written.")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
