"""Tests for the anonymiser in biwenger_capture.py, on made-up data. No network.

Run from the repo root: python3 -m unittest discover -s scripts
"""

import contextlib
import copy
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import biwenger_capture
from biwenger_capture import Anonymiser, trim_catalog


def sample_responses():
    me = {"id": 5551234, "name": "Los Galácticos", "icon": "https://cdn/u/5551234.png", "balance": 1200000}
    rival = {"id": 5559876, "name": "Rival FC", "icon": "https://cdn/u/5559876.png", "points": 80,
             "twitter": "rival_handle"}
    league = {"id": 7770001, "name": "Liga de los Amigos", "competition": "la-liga", "scoreID": 5,
              "icon": "https://cdn/l/7770001.png", "user": me,
              "settings": {"secret": "invite-abc123", "clause": "on"}}
    account = {"data": {
        "account": {"id": 9990001, "name": "Jane Real", "email": "jane@example.com", "phone": "+34600000000",
                    "birthday": 631152000, "devices": [{"type": "ios", "token": "push-token-xyz", "updated": 1}]},
        "leagues": [league],
        "location": {"country": "ES", "region": "Madrid", "city": "Madrid"},
    }}
    league_response = {"data": {"id": 7770001, "name": "Liga de los Amigos",
                                "users": [dict(me), dict(rival)]}}
    board = {"data": [{"type": "transfer", "content": [{"player": 101, "from": dict(rival), "to": dict(me),
                                                        "amount": 500000}],
                       "title": "Rival FC vendió a Los Galácticos"}]}
    catalog = {"data": {"players": {"101": {"id": 101, "name": "Pedri", "slug": "pedri", "teamID": 3,
                                            "twitter": "Pedri"},
                                    "102": {"id": 102, "name": "Vinícius Júnior", "slug": "vinicius-junior"}},
                        "teams": {"3": {"id": 3, "name": "Barcelona", "slug": "barcelona"},
                                  "15": {"id": 15, "name": "Real Madrid", "slug": "real-madrid"}}}}
    return league, {"account": account, "league": league_response, "board": board, "catalog": catalog}


def anonymise(league, responses):
    anonymiser = Anonymiser()
    anonymiser.add_manager(league["user"])
    anonymiser.add_account(responses["account"]["data"]["account"])
    anonymiser.add_league(league, 1)
    for response in responses.values():
        anonymiser.find_managers(response)
        anonymiser.collect_secrets(response)
    out = {name: anonymiser.apply(response) for name, response in responses.items()}
    return anonymiser, out


class AnonymiserTest(unittest.TestCase):

    def setUp(self):
        league, responses = sample_responses()
        self.anonymiser, self.out = anonymise(league, responses)
        self.text = json.dumps(self.out, ensure_ascii=False)

    def test_removes_personal_fields_but_keeps_their_type(self):
        account = self.out["account"]["data"]["account"]
        self.assertEqual(account["email"], "removed")
        self.assertEqual(account["birthday"], 0)
        self.assertEqual(account["devices"], [])
        self.assertEqual(self.out["account"]["data"]["location"], {})
        self.assertEqual(self.out["account"]["data"]["leagues"][0]["settings"]["secret"], "removed")
        self.assertEqual(self.out["league"]["data"]["users"][1]["twitter"], "removed")

    def test_you_are_manager_1_and_names_are_consistent_across_responses(self):
        users = self.out["league"]["data"]["users"]
        self.assertEqual([u["name"] for u in users], ["Manager 1", "Manager 2"])
        self.assertEqual(self.out["account"]["data"]["leagues"][0]["user"]["name"], "Manager 1")
        transfer = self.out["board"]["data"][0]["content"][0]
        self.assertEqual((transfer["from"]["name"], transfer["to"]["name"]), ("Manager 2", "Manager 1"))

    def test_replaces_ids_in_numbers_and_text(self):
        users = self.out["league"]["data"]["users"]
        self.assertEqual([u["id"] for u in users], [2000001, 2000002])
        self.assertEqual(self.out["league"]["data"]["id"], 1000001)
        self.assertEqual(self.out["account"]["data"]["account"]["id"], 3000001)
        self.assertEqual(users[0]["icon"], "https://example.invalid/icon.png")

    def test_replaces_names_inside_free_text(self):
        self.assertEqual(self.out["board"]["data"][0]["title"], "Manager 2 vendió a Manager 1")

    def test_keeps_public_football_data(self):
        players = self.out["catalog"]["data"]["players"]
        self.assertEqual(players["101"]["name"], "Pedri")
        self.assertEqual(players["101"]["twitter"], "Pedri")
        self.assertEqual(self.out["catalog"]["data"]["teams"]["3"]["name"], "Barcelona")
        # The account's city is removed, but the same word in public data is not a leak.
        self.assertEqual(self.out["catalog"]["data"]["teams"]["15"]["name"], "Real Madrid")
        self.assertEqual(self.out["board"]["data"][0]["content"][0]["player"], 101)

    def test_output_has_no_leaks(self):
        self.assertEqual(self.anonymiser.leaks(self.text, extra=("jane@example.com",)), [])
        for real in ("jane@example.com", "push-token-xyz", "invite-abc123", "Jane Real", "Los Galácticos",
                     "Rival FC", "rival_handle", "Liga de los Amigos", "5551234", "7770001"):
            self.assertNotIn(real, self.text)

    def test_leak_check_names_what_is_left_without_showing_it(self):
        leaky = self.text + ' "Rival FC" 5559876 invite-abc123 jane@example.com'
        self.assertEqual(self.anonymiser.leaks(leaky, extra=("jane@example.com",)), [
            "manager or league name", "manager, league or account id",
            "a value of a removed field (email, secret)", "email, password or token"])


class TrimCatalogTest(unittest.TestCase):

    def test_keeps_wanted_players_first_then_a_few_more(self):
        catalog = {"data": {"players": {str(i): {"id": i} for i in range(1, 101)}},
                   "meta": {"config": {"ads": {}}, "sports": [{"id": "football"}]}}
        trimmed, total = trim_catalog(catalog, keep_ids=[50, 99])
        self.assertEqual(total, 100)
        self.assertEqual(list(trimmed["data"]["players"])[:2], ["50", "99"])
        self.assertEqual(len(trimmed["data"]["players"]), 12)
        self.assertEqual(trimmed["meta"], {"sports": [{"id": "football"}]})


class SharedNameTest(unittest.TestCase):

    def test_your_team_is_manager_1_when_it_shares_your_account_name(self):
        league, responses = sample_responses()
        responses["account"]["data"]["account"]["name"] = league["user"]["name"]
        _, out = anonymise(league, responses)
        self.assertEqual(out["account"]["data"]["leagues"][0]["user"]["name"], "Manager 1")
        self.assertEqual(out["league"]["data"]["users"][0]["name"], "Manager 1")


class CdnTest(unittest.TestCase):

    def test_cdn_mode_needs_no_login_and_zeroes_birthdays(self):
        seen = []

        def request(client, name, url, body=None):
            seen.append((name, client.headers.get("Authorization"), body))
            return {"status": 200, "data": {"id": 1, "name": "Pedri", "birthday": 1036454400, "reports": []}}

        with tempfile.TemporaryDirectory() as tmp, \
                mock.patch.object(biwenger_capture.Biwenger, "request", request), \
                mock.patch.object(biwenger_capture, "load_credentials") as credentials, \
                contextlib.redirect_stdout(io.StringIO()):
            status = biwenger_capture.main(root=Path(tmp), out_dir=Path(tmp) / "samples", argv=["--cdn"])
            files = {f.stem: json.loads(f.read_text()) for f in (Path(tmp) / "samples").glob("*.json")}

        self.assertEqual(status, 0)
        credentials.assert_not_called()
        self.assertEqual(sorted(files), sorted(biwenger_capture.CDN_SAMPLES))
        self.assertEqual({auth for _, auth, _ in seen}, {None})
        self.assertEqual({body for _, _, body in seen}, {None})
        self.assertEqual(files["player-reports"]["data"]["birthday"], 0)


class MainTest(unittest.TestCase):
    """The whole run against fake HTTP responses: what is written and what is printed."""

    def test_writes_clean_samples_and_prints_nothing_real(self):
        league, responses = sample_responses()
        fake = {
            "login": {"token": "secret-token-123"},
            "account": responses["account"],
            "league": responses["league"],
            "user-squad": {"data": {"players": [{"id": 101, "owner": {"price": 1}}]}},
            "market": {"data": {"sales": [{"player": {"id": 102}, "user": None, "price": 5}]}},
            "catalog": responses["catalog"],
            "rounds-league": {"data": {"league": {"standings": [dict(league["user"])]}}},
            "board": responses["board"],
            "player-prices": {"data": {"id": 101, "name": "Pedri", "prices": [[260101, 100]]}},
            "player-reports": {"data": {"id": 101, "name": "Pedri", "reports": []}},
        }
        seen = []

        def request(client, name, url, body=None):
            seen.append((name, client.headers.get("Authorization")))
            return copy.deepcopy(fake[name])

        with tempfile.TemporaryDirectory() as tmp, \
                mock.patch.object(biwenger_capture.Biwenger, "request", request), \
                mock.patch.object(biwenger_capture, "load_credentials", return_value=("jane@example.com", "pw-1234")), \
                contextlib.redirect_stdout(io.StringIO()) as printed:
            status = biwenger_capture.main(root=Path(tmp), out_dir=Path(tmp) / "samples", argv=[])
            files = {f.stem: f.read_text() for f in (Path(tmp) / "samples").glob("*.json")}

        self.assertEqual(status, 0)
        self.assertEqual(sorted(files), sorted(name for name in fake if name != "login"))
        everything = "".join(files.values()) + printed.getvalue()
        for real in ("jane@example.com", "pw-1234", "secret-token-123", "invite-abc123", "Jane Real",
                     "Los Galácticos", "Rival FC", "Liga de los Amigos", "5551234", "5559876", "7770001"):
            self.assertNotIn(real, everything)
        # The CDN calls never carry the token.
        self.assertEqual([auth for name, auth in seen if name.startswith("player-")], [None, None])


if __name__ == "__main__":
    unittest.main()
