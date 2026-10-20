#!/usr/bin/env bash
# PreToolUse hook (Bash): block dangerous commands before they run.
#   1. Force-pushing to main (--force, -f, --force-with-lease, or a +main refspec,
#      or any force push while main is checked out).
#   2. Printing .env files (they hold Biwenger credentials). .env.example is fine.
# Exit code 2 blocks the command and shows the message to Claude.

input=$(cat)
cmd=$(printf '%s' "$input" | python3 -c 'import json,sys; print((json.load(sys.stdin).get("tool_input") or {}).get("command",""))')

block() {
  echo "Blocked by .claude/hooks/guard-bash.sh: $1" >&2
  exit 2
}

if printf '%s' "$cmd" | grep -Eq '(^|[;&|[:space:]])git[[:space:]]+([^;&|]*[[:space:]])?push([[:space:]]|$)'; then
  push=$(printf '%s' "$cmd" | grep -Eo 'git[[:space:]]+([^;&|]*[[:space:]])?push[^;&|]*')
  if printf '%s' "$push" | grep -Eq '[[:space:]](--force(-with-lease)?(=[^[:space:]]*)?|-[a-zA-Z]*f[a-zA-Z]*)([[:space:]]|$)|[[:space:]]\+[^[:space:]]*'; then
    branch=$(git -C "${CLAUDE_PROJECT_DIR:-.}" branch --show-current 2>/dev/null)
    if printf '%s' "$push" | grep -Eq '(^|[[:space:]:+])main([[:space:]]|$)' || [ "$branch" = "main" ]; then
      block "force-pushing to main is not allowed. Push a branch and open a PR instead."
    fi
  fi
fi

without_example=$(printf '%s' "$cmd" | sed 's/\.env\.example//g')
if printf '%s' "$without_example" | grep -Eq '(^|[;&|[:space:]])(cat|less|more|head|tail|bat|grep|egrep|rg|awk|sed|cut|sort|xxd|od|strings|nl|tac|source|\.|base64)[[:space:]]+([^;&|]*[[:space:]/])?\.env(\.[a-zA-Z0-9_-]+)?([[:space:]]|$|[;&|])' \
  || printf '%s' "$without_example" | grep -Eq '(^|[;&|[:space:]])cp[[:space:]]+([^;&|]*[[:space:]/])?\.env(\.[a-zA-Z0-9_-]+)?[[:space:]]+[^;&|[:space:]]'; then
  block "reading .env files is not allowed, they contain secrets. Use .env.example to see which variables exist."
fi

exit 0
