#!/usr/bin/env python3
"""Extract anonymised samples of Biwenger *write* requests from a browser HAR file (issue #19).

Run it yourself from the repo root:  python3 scripts/biwenger_har_extract.py ~/Downloads/biwenger.har
Several HAR files from the same session can be given at once: the account call from any of them
anonymises all of them, and the samples are numbered in order across the files.
With --list it only prints what the HAR holds (methods, paths with numbers masked, status, whether
the answer was saved), to find out why an extraction fails.

The HAR holds your session: keep it outside the repo and delete it afterwards. This script:
- keeps only non-GET requests to biwenger.as.com/api/ (the writes). The login is skipped: its body
  holds the password;
- drops every Authorization, Cookie and Set-Cookie value, and keeps only a short list of other
  request headers (the rest are listed by name only);
- anonymises bodies, paths and headers like the read samples (you are Manager 1, ...), using the
  account call in the same HAR, so reload the page after you start recording;
- checks each file for leaks (names, ids, email, token, cookies, password) before writing it;
- prints methods, anonymised paths and status codes only. No bodies, no real ids.

Samples go to docs/biwenger-api/samples/write/. Standard library only.
"""

import argparse
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit

from biwenger_capture import Anonymiser

API_HOST = "biwenger.as.com"
LOGIN_PATH = "/api/v2/auth/login"
ACCOUNT_PATH = "/api/v2/account"
SECRET_HEADERS = {"authorization", "cookie", "set-cookie"}
# Request headers worth seeing with their (anonymised) value. Others appear by name only.
KEPT_HEADERS = {"content-type", "accept", "x-lang", "x-league", "x-user", "x-version"}


def parse_json(text):
    if not text:
        return None
    try:
        return json.loads(text)
    except (TypeError, ValueError):
        return None


def entries(har):
    """(method, path, query, request, response) for every API call in the HAR."""
    for entry in (har.get("log") or {}).get("entries") or []:
        request = entry.get("request") or {}
        url = urlsplit(request.get("url", ""))
        if url.hostname != API_HOST or not url.path.startswith("/api/"):
            continue
        yield request.get("method", "").upper(), url.path, url.query, request, entry.get("response") or {}


def body_text(request):
    return ((request.get("postData") or {}).get("text")) or ""


def response_text(response):
    return ((response.get("content") or {}).get("text")) or ""


def secret_values(har):
    """Values that must never appear in a sample: auth headers, cookies and the login body."""
    values = set()
    for method, path, _, request, response in entries(har):
        for header in (request.get("headers") or []) + (response.get("headers") or []):
            if header.get("name", "").lower() in SECRET_HEADERS:
                value = header.get("value", "")
                values.add(value)
                values.update(part.strip() for part in re.split(r"[;=\s]", value))
        for cookie in (request.get("cookies") or []) + (response.get("cookies") or []):
            values.add(cookie.get("value", ""))
        if path == LOGIN_PATH:
            login = parse_json(body_text(request)) or {}
            values.update(str(v) for v in login.values())
            values.update(str(v) for v in (parse_json(response_text(response)) or {}).values())
    return {v for v in values if v and len(v) >= 8}


def build_anonymiser(har):
    account = None
    for method, path, _, _, response in entries(har):
        if method == "GET" and path == ACCOUNT_PATH:
            account = parse_json(response_text(response))
    if not account:
        sys.exit(
            "The HAR has no GET /api/v2/account answer, so ids and names can't be anonymised.\n"
            "Start recording, reload biwenger.as.com, then do the actions and export again.")
    data = account.get("data") or {}
    leagues = data.get("leagues") or []
    la_liga = [league for league in leagues if league.get("competition") == "la-liga"]
    anonymiser = Anonymiser()
    if len(la_liga) == 1 and isinstance(la_liga[0].get("user"), dict):
        anonymiser.add_manager(la_liga[0]["user"])  # you are Manager 1, as in the read samples
    anonymiser.add_account(data.get("account") or {})
    for n, league in enumerate(la_liga + [lg for lg in leagues if lg not in la_liga], start=1):
        anonymiser.add_league(league, n)
    for _, _, _, request, response in entries(har):
        for value in (parse_json(body_text(request)), parse_json(response_text(response))):
            if value is not None:
                anonymiser.find_managers(value)
                anonymiser.collect_secrets(value)
    return anonymiser


def sample(anonymiser, method, path, query, request, response):
    kept, other = {}, []
    for header in request.get("headers") or []:
        name = header.get("name", "")
        lowered = name.lower()
        if lowered in SECRET_HEADERS or lowered.startswith(":"):
            continue
        if lowered in KEPT_HEADERS:
            kept[name] = anonymiser.apply(header.get("value", ""))
        else:
            other.append(name)
    request_body = parse_json(body_text(request))
    response_body = parse_json(response_text(response))
    return {
        "method": method,
        "path": anonymiser.apply(path),
        "query": anonymiser.apply(query),
        "requestHeaders": kept,
        "otherRequestHeaderNames": sorted(set(other), key=str.lower),
        "requestBody": anonymiser.apply(request_body) if request_body is not None
        else anonymiser.apply(body_text(request)) or None,
        "status": response.get("status"),
        "responseBody": anonymiser.apply(response_body) if response_body is not None
        else anonymiser.apply(response_text(response)) or None,
    }


def file_name(index, method, anonymised_path):
    slug = re.sub(r"[^a-z0-9]+", "-", anonymised_path.lower().removeprefix("/api/v2/")).strip("-")
    return f"{index:02d}-{method.lower()}-{slug or 'root'}.json"


REPO = Path(__file__).resolve().parent.parent


def list_entries(har):
    """What the HAR holds, without anything that identifies you: numbers are masked."""
    hosts = {}
    for entry in (har.get("log") or {}).get("entries") or []:
        host = urlsplit((entry.get("request") or {}).get("url", "")).hostname or "?"
        hosts[host] = hosts.get(host, 0) + 1
    print("Requests per host:")
    for host, count in sorted(hosts.items(), key=lambda item: -item[1]):
        print(f"  {count:>5}  {host}")
    print(f"\nCalls to {API_HOST}/api/ (numeric path parts masked):")
    for method, path, _, _, response in entries(har):
        saved = "answer saved" if response_text(response) else "NO answer saved"
        print(f"  {method:<6} {re.sub(r'(?<=/)[0-9]+(?=/|$)', 'N', path):<45} HTTP {response.get('status')}  {saved}")


def main(argv=None, out_dir=None):
    parser = argparse.ArgumentParser(description="Extract anonymised Biwenger write samples.")
    parser.add_argument("har", type=Path, nargs="+", help="HAR file(s) exported from DevTools")
    parser.add_argument("--list", action="store_true", help="only list what the HAR holds")
    args = parser.parse_args(sys.argv[1:] if argv is None else argv)
    out_dir = out_dir or REPO / "docs/biwenger-api/samples/write"

    for path in args.har:
        if path.resolve().is_relative_to(REPO):
            sys.exit(
                f"{path} is inside the repo, where it could be committed by accident, and it holds\n"
                f"your session. Move it out first, for example: mv {path} ~/Downloads/biwenger.har")
    # One log with every file's entries, in the order given.
    har = {"log": {"entries": [
        entry
        for path in args.har
        for entry in (json.loads(path.read_text(encoding="utf-8")).get("log") or {}).get("entries")
        or []]}}
    if args.list:
        list_entries(har)
        return 0
    anonymiser = build_anonymiser(har)
    secrets = secret_values(har)

    writes = [(m, p, q, rq, rs) for m, p, q, rq, rs in entries(har) if m not in ("GET", "OPTIONS")]
    print(f"{len(writes)} write request(s) found:")
    out_dir.mkdir(parents=True, exist_ok=True)
    # Each run writes the whole set: earlier samples would otherwise mix with this run's numbering.
    for old in out_dir.glob("[0-9][0-9]-*.json"):
        old.unlink()
    failed, index = 0, 0
    for method, path, query, request, response in writes:
        if path == LOGIN_PATH:
            print(f"  {method:<6} {path:<40} skipped (login: its body holds the password)")
            continue
        index += 1
        record = sample(anonymiser, method, path, query, request, response)
        text = json.dumps(record, indent=2, ensure_ascii=False) + "\n"
        leaks = anonymiser.leaks(text, extra=secrets)
        label = f"  {method:<6} {record['path']:<40} HTTP {record['status']}"
        if leaks:
            failed += 1
            print(f"{label}  NOT written: still contains {', '.join(leaks)}")
            continue
        name = file_name(index, method, record["path"])
        (out_dir / name).write_text(text)
        print(f"{label}  written: {name}")
    print(f"\n{index} sample(s), {failed} not written. Now delete the HAR file: it holds your session.")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
