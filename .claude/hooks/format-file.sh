#!/usr/bin/env bash
# PostToolUse hook (Edit|Write): format the file Claude just changed.
#   *.java                      -> Spotless, via the Maven wrapper in backend/
#   *.ts/.tsx/.js/.jsx/.css/.json/.md under frontend/ -> Prettier from frontend/node_modules
# Does nothing until those tools exist (Phase 1) and never fails the edit.

input=$(cat)
file=$(printf '%s' "$input" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("tool_input") or {}).get("file_path") or (d.get("tool_response") or {}).get("filePath") or "")')
[ -n "$file" ] && [ -f "$file" ] || exit 0

root="${CLAUDE_PROJECT_DIR:-$(git -C "$(dirname "$file")" rev-parse --show-toplevel 2>/dev/null)}"
[ -n "$root" ] || exit 0

case "$file" in
  *.java)
    if [ -x "$root/backend/mvnw" ]; then
      # Use the project's JDK from backend/.sdkmanrc when SDKMAN has it, whatever the default java is.
      jdk="$HOME/.sdkman/candidates/java/$(sed -n 's/^java=//p' "$root/backend/.sdkmanrc" 2>/dev/null)"
      [ -x "$jdk/bin/java" ] && export JAVA_HOME="$jdk"
      (cd "$root/backend" && ./mvnw -q spotless:apply -DspotlessFiles="$(printf '%s' "$file" | sed 's/[.[\*^$()+?{|]/\\&/g')") >/dev/null 2>&1
    fi
    ;;
  "$root"/frontend/*.ts|"$root"/frontend/*.tsx|"$root"/frontend/*.js|"$root"/frontend/*.jsx|"$root"/frontend/*.css|"$root"/frontend/*.json|"$root"/frontend/*.md)
    if [ -x "$root/frontend/node_modules/.bin/prettier" ]; then
      # Run from frontend/: Prettier resolves its plugins (Tailwind class sorting) from the current directory.
      (cd "$root/frontend" && ./node_modules/.bin/prettier --write --log-level warn "$file") >/dev/null 2>&1
    fi
    ;;
esac
exit 0
