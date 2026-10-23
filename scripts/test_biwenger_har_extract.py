"""Tests for biwenger_har_extract.py, on a made-up HAR. No network.

Run from the repo root: python3 -m unittest discover -s scripts
"""

import base64
import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path

import biwenger_har_extract

TOKEN = "eyJhbGciOiJIUzI1NiJ9.secret-token-value"
COOKIE = "session=abcdef1234567890"


def entry(method, path, request_body=None, response_body=None, status=200, query=""):
    url = f"https://biwenger.as.com{path}" + (f"?{query}" if query else "")
    request = {
        "method": method,
        "url": url,
        "headers": [
            {"name": "Authorization", "value": f"Bearer {TOKEN}"},
            {"name": "Cookie", "value": COOKIE},
            {"name": "Content-Type", "value": "application/json"},
            {"name": "X-League", "value": "7770001"},
            {"name": "X-User", "value": "5551234"},
            {"name": "X-Version", "value": "631"},
            {"name": "Sec-Fetch-Mode", "value": "cors"},
        ],
        "cookies": [{"name": "session", "value": "abcdef1234567890"}],
    }
    if request_body is not None:
        request["postData"] = {"mimeType": "application/json", "text": json.dumps(request_body)}
    return {
        "request": request,
        "response": {
            "status": status,
            "headers": [{"name": "Set-Cookie", "value": "other=zyxwvu9876543210"}],
            "content": {"text": json.dumps(response_body) if response_body is not None else ""},
        },
    }


def har(*entries):
    return {"log": {"entries": list(entries)}}


ME = {"id": 5551234, "name": "Los Galácticos", "icon": "https://cdn/u/5551234.png", "balance": 1}
ACCOUNT = {"status": 200, "data": {
    "account": {"id": 9990001, "name": "Jane Real", "email": "jane@example.com"},
    "leagues": [{"id": 7770001, "name": "Liga de los Amigos", "competition": "la-liga",
                 "user": ME, "settings": {"secret": "invite-abc123"}}]}}


def run(har_data, *options):
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "capture.har"
        path.write_text(json.dumps(har_data))
        out = Path(tmp) / "samples"
        with contextlib.redirect_stdout(io.StringIO()) as printed:
            status = biwenger_har_extract.main([str(path), *options], out_dir=out)
        files = {f.name: f.read_text() for f in sorted(out.glob("*.json"))} if out.exists() else {}
    return status, files, printed.getvalue()


class HarExtractTest(unittest.TestCase):

    def setUp(self):
        self.status, self.files, self.printed = run(har(
            entry("POST", "/api/v2/auth/login", {"email": "jane@example.com", "password": "pw-secret-1"},
                  {"token": TOKEN}),
            entry("GET", "/api/v2/account", response_body=ACCOUNT),
            entry("GET", "/api/v2/market", response_body={"status": 200, "data": {"sales": []}}),
            entry("POST", "/api/v2/market/bid", {"player": 101, "amount": 150000, "user": 5551234},
                  {"status": 200, "data": {"id": 42, "user": dict(ME)}}),
            entry("DELETE", "/api/v2/league/7770001/bid/42", response_body={"status": 200}),
        ))
        self.everything = "".join(self.files.values()) + self.printed

    def test_writes_only_the_write_requests_and_skips_the_login(self):
        self.assertEqual(self.status, 0)
        self.assertEqual(sorted(self.files), ["01-post-market-bid.json", "02-delete-league-1000001-bid-42.json"])
        self.assertIn("skipped (login", self.printed)

    def test_keeps_method_body_status_and_anonymised_headers(self):
        bid = json.loads(self.files["01-post-market-bid.json"])
        self.assertEqual(bid["method"], "POST")
        self.assertEqual(bid["path"], "/api/v2/market/bid")
        self.assertEqual(bid["requestBody"], {"player": 101, "amount": 150000, "user": 2000001})
        self.assertEqual(bid["responseBody"]["data"]["user"]["name"], "Manager 1")
        self.assertEqual(bid["requestHeaders"], {
            "Content-Type": "application/json", "X-League": "1000001", "X-User": "2000001", "X-Version": "631"})
        self.assertEqual(bid["otherRequestHeaderNames"], ["Sec-Fetch-Mode"])

    def test_no_secret_or_real_id_survives(self):
        for real in (TOKEN, "abcdef1234567890", "zyxwvu9876543210", "pw-secret-1", "jane@example.com",
                     "Jane Real", "Los Galácticos", "Liga de los Amigos", "5551234", "7770001", "invite-abc123"):
            self.assertNotIn(real, self.everything)

    def test_refuses_to_run_without_the_account_call(self):
        with self.assertRaises(SystemExit) as stop:
            run(har(entry("POST", "/api/v2/market/bid", {"player": 101})))
        self.assertIn("reload biwenger.as.com", str(stop.exception.code))

    def test_a_file_with_a_leak_is_not_written(self):
        # The token pasted into a body under a key the anonymiser doesn't know.
        leaky = entry("PUT", "/api/v2/user/lineup", {"note": "lineup", "token": TOKEN})
        leaky["request"]["postData"]["text"] = leaky["request"]["postData"]["text"].replace(
            '"token": "' + TOKEN + '"', '"t": "' + TOKEN + '"')
        status, files, printed = run(har(entry("GET", "/api/v2/account", response_body=ACCOUNT), leaky))
        self.assertEqual(status, 1)
        self.assertEqual(files, {})
        self.assertIn("NOT written", printed)
        self.assertNotIn(TOKEN, printed)


class SeveralFilesTest(unittest.TestCase):

    def test_the_account_in_one_file_anonymises_the_other(self):
        with tempfile.TemporaryDirectory() as tmp:
            first, second = Path(tmp) / "a.har", Path(tmp) / "b.har"
            first.write_text(json.dumps(har(
                entry("GET", "/api/v2/account", response_body=ACCOUNT),
                entry("POST", "/api/v2/offers", {"amount": 150000, "to": 5551234}))))
            second.write_text(json.dumps(har(
                entry("PUT", "/api/v2/user", {"lineup": {"players": [101]}, "user": 5551234}))))
            out = Path(tmp) / "samples"
            with contextlib.redirect_stdout(io.StringIO()):
                status = biwenger_har_extract.main([str(first), str(second)], out_dir=out)
            files = {f.name: f.read_text() for f in sorted(out.glob("*.json"))}

        self.assertEqual(status, 0)
        self.assertEqual(sorted(files), ["01-post-offers.json", "02-put-user.json"])
        self.assertIn('"user": 2000001', files["02-put-user.json"])
        self.assertNotIn("5551234", "".join(files.values()))


class EncodingTest(unittest.TestCase):

    def test_reads_answers_stored_as_base64(self):
        account = entry("GET", "/api/v2/account")
        account["response"]["content"] = {
            "encoding": "base64",
            "text": base64.b64encode(json.dumps(ACCOUNT).encode()).decode()}
        status, files, _ = run(har(account, entry("POST", "/api/v2/offers", {"to": 5551234})))
        self.assertEqual(status, 0)
        self.assertIn('"to": 2000001', files["01-post-offers.json"])

    def test_an_unreadable_account_answer_is_named_as_such(self):
        account = entry("GET", "/api/v2/account")
        account["response"]["content"] = {"text": "<html>not json</html>"}
        with self.assertRaises(SystemExit) as stop:
            run(har(account))
        self.assertIn("can't be read as JSON", str(stop.exception.code))


class SafetyTest(unittest.TestCase):

    def test_refuses_a_har_inside_the_repo(self):
        inside = biwenger_har_extract.REPO / "scripts" / "capture.har"
        with self.assertRaises(SystemExit) as stop, contextlib.redirect_stdout(io.StringIO()):
            biwenger_har_extract.main([str(inside)])
        self.assertIn("inside the repo", str(stop.exception.code))

    def test_list_masks_numbers_and_shows_no_bodies(self):
        status, files, printed = run(har(
            entry("GET", "/api/v2/league/7770001", response_body={"data": {"name": "Liga de los Amigos"}}),
            entry("POST", "/api/v2/market/bid", {"amount": 150000})), "--list")
        self.assertEqual(status, 0)
        self.assertEqual(files, {})
        self.assertIn("GET    /api/v2/league/N", printed)
        self.assertIn("answer saved", printed)
        for real in ("7770001", "Liga de los Amigos", "150000", TOKEN):
            self.assertNotIn(real, printed)


if __name__ == "__main__":
    unittest.main()
