"""Tests for the hook commands as configured: settings.json and each agent's frontmatter.

They run the real command strings with CLAUDE_PROJECT_DIR set to the repo root, to a subfolder
(a session started in frontend/ once made every hook miss its script) and to a folder outside the
repo, where a guard must block instead of letting calls through.

Run from the repo root: python3 -m unittest discover -s .claude/hooks
"""

import json
import os
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SUBFOLDERS = [REPO, REPO / "frontend", REPO / "backend"]
BLOCK = 2  # the exit code Claude Code treats as "block this tool call"


def settings_command(event):
    settings = json.loads((REPO / ".claude/settings.json").read_text())
    return settings["hooks"][event][0]["hooks"][0]["command"]


def agent_commands():
    """Each agent's hook command, from the single-quoted `command:` line in its frontmatter."""
    commands = {}
    for agent in sorted((REPO / ".claude/agents").glob("*.md")):
        match = re.search(r"^\s*command: '(.*)'$", agent.read_text(), re.M)
        if match:
            commands[agent.stem] = match.group(1).replace("''", "'")
    return commands


def run(command, project_dir, tool_input):
    """Runs a hook command the way Claude Code does: in a shell, the tool call as JSON on stdin."""
    env = {**os.environ, "CLAUDE_PROJECT_DIR": str(project_dir)}
    return subprocess.run(
        ["sh", "-c", command],
        input=json.dumps(tool_input),
        capture_output=True,
        text=True,
        cwd=project_dir,
        env=env,
        timeout=30,
    )


def bash(command):
    return {"tool_name": "Bash", "tool_input": {"command": command}}


class GuardBashTest(unittest.TestCase):
    command = settings_command("PreToolUse")

    def test_runs_from_the_root_and_from_subfolders(self):
        for folder in SUBFOLDERS:
            with self.subTest(folder=folder.name):
                self.assertEqual(run(self.command, folder, bash("git status")).returncode, 0)
                blocked = run(self.command, folder, bash("git push --force origin main"))
                self.assertEqual(blocked.returncode, BLOCK)
                self.assertIn("guard-bash.sh: force-pushing", blocked.stderr)

    def test_blocks_when_the_script_cannot_be_found(self):
        with tempfile.TemporaryDirectory() as outside:
            result = run(self.command, outside, bash("git status"))
        self.assertEqual(result.returncode, BLOCK)
        self.assertIn("guard-bash.sh not found", result.stderr)


class AgentGuardTest(unittest.TestCase):
    commands = agent_commands()

    def test_every_agent_has_a_guard(self):
        self.assertEqual(
            sorted(self.commands),
            ["biwenger-api-explorer", "java-reviewer", "react-reviewer", "test-writer",
             "tutorial-scribe"],
        )

    def test_runs_from_the_root_and_from_subfolders(self):
        for agent, command in self.commands.items():
            for folder in SUBFOLDERS:
                with self.subTest(agent=agent, folder=folder.name):
                    self.assertEqual(run(command, folder, bash("git status")).returncode, 0)
                    blocked = run(command, folder, bash("rm -rf build"))
                    self.assertEqual(blocked.returncode, BLOCK)
                    self.assertIn("agent-guard.py: 'rm -rf build' is not allowed", blocked.stderr)

    def test_blocks_when_the_script_cannot_be_found(self):
        with tempfile.TemporaryDirectory() as outside:
            for agent, command in self.commands.items():
                with self.subTest(agent=agent):
                    result = run(command, outside, bash("git status"))
                    self.assertEqual(result.returncode, BLOCK)
                    self.assertIn("agent-guard.py not found", result.stderr)


class FormatFileTest(unittest.TestCase):
    command = settings_command("PostToolUse")

    def test_runs_from_the_root_and_from_subfolders(self):
        # A file that doesn't exist: the script exits 0 at once, so nothing is formatted.
        edit = {"tool_name": "Write", "tool_input": {"file_path": "/nonexistent/File.java"}}
        for folder in SUBFOLDERS:
            with self.subTest(folder=folder.name):
                self.assertEqual(run(self.command, folder, edit).returncode, 0)

    def test_warns_without_blocking_when_the_script_cannot_be_found(self):
        # The edit has already happened: say the file wasn't formatted, but don't block.
        edit = {"tool_name": "Write", "tool_input": {"file_path": "/nonexistent/File.java"}}
        with tempfile.TemporaryDirectory() as outside:
            result = run(self.command, outside, edit)
        self.assertNotIn(result.returncode, (0, BLOCK))
        self.assertIn("format-file.sh not found", result.stderr)


if __name__ == "__main__":
    unittest.main()
