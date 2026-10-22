#!/usr/bin/env python3
"""PreToolUse hook for project subagents: limit what each agent may run or edit.

Usage (from an agent's frontmatter): agent-guard.py <profile>
Profiles: java-reviewer, react-reviewer, test-writer, biwenger-api-explorer, tutorial-scribe.
Exit code 2 blocks the tool call and shows the message to the agent.
"""
import json
import re
import sys

# Read-only helpers every profile may use in a pipeline.
COMMON = [
    r"cd \S+", r"ls( .*)?", r"pwd", r"true", r"echo( .*)?",
    r"cat .+", r"head( .*)?", r"tail( .*)?", r"grep .+", r"wc( .*)?", r"sort( .*)?", r"uniq( .*)?",
    r"git (status|diff|log|show|rev-parse|merge-base)( .*)?",
]
MAVEN = [r"(JAVA_HOME=\S+ )?(\./|\S*/)?mvnw( .*)?"]  # JAVA_HOME prefix: backend needs JDK 25
NPM = [
    r"npm (test|ci)( .*)?", r"npm run (test|lint|build|typecheck|format:check)( .*)?",
    r"npx (vitest|eslint|tsc|prettier --check)(?!.*--fix)( .*)?",  # --fix would change files
]
GH_READ = [r"gh (issue|pr) (view|list|diff)( .*)?", r"gh pr checks( .*)?"]
# GET requests only: no method override, no request body, no uploads.
CURL_GET = [r"curl (?!(.*\s)?(-X(?!\s*GET\b)|--request(?!\s*GET\b)|-d\b|--data|-F\b|--form|-T\b|--upload-file|--json)).+"]

TEST_PATHS = [
    r"/src/test/",                        # Java tests and test resources
    r"\.(test|spec)\.[jt]sx?$",          # Vitest / Playwright files
    r"/__tests__/", r"/frontend/src/test/", r"/frontend/e2e/",
]

PROFILES = {
    "java-reviewer": {"bash": COMMON + MAVEN, "write": []},
    "react-reviewer": {"bash": COMMON + NPM, "write": []},
    "test-writer": {"bash": COMMON + MAVEN + NPM, "write": TEST_PATHS},
    "biwenger-api-explorer": {"bash": COMMON + CURL_GET, "write": [r"/docs/biwenger-api/"]},
    "tutorial-scribe": {"bash": COMMON + GH_READ, "write": [r"/docs/tutorial/"]},
}


def block(msg):
    print(f"Blocked by .claude/hooks/agent-guard.py: {msg}", file=sys.stderr)
    sys.exit(2)


def check_bash(cmd, allowed):
    # Redirections to files and command substitution could write anywhere or hide commands.
    stripped = re.sub(r"\d?>\s*/dev/null|2>&1", "", cmd)
    if re.search(r"[<>`]|\$\(", stripped):
        block("redirections and command substitution are not allowed for this agent.")
    for segment in re.split(r"&&|\|\||[;|\n]", cmd):
        segment = re.sub(r"\d?>\s*/dev/null|2>&1", "", segment).strip()
        if segment and not any(re.fullmatch(p, segment) for p in allowed):
            block(f"'{segment}' is not allowed for this agent.")


def main():
    profile = PROFILES.get(sys.argv[1] if len(sys.argv) > 1 else "")
    if profile is None:
        block(f"unknown profile {sys.argv[1:]}.")
    data = json.load(sys.stdin)
    tool = data.get("tool_name", "")
    tool_input = data.get("tool_input") or {}
    if tool == "Bash":
        check_bash(tool_input.get("command", ""), profile["bash"])
    elif tool in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
        path = tool_input.get("file_path") or tool_input.get("notebook_path") or ""
        if not any(re.search(p, path) for p in profile["write"]):
            block(f"this agent may not edit {path}.")
    sys.exit(0)


if __name__ == "__main__":
    main()
