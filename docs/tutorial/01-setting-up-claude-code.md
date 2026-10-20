# 01 — Setting up Claude Code

Phase 0 builds no application code. It sets up the repo so that every future Claude Code session starts with the right knowledge, the right permissions and the right guardrails. This chapter explains each piece, why it's there and how to check it works.

Issues: [#2](https://github.com/DavidMS/biwenger-manager/issues/2) `CLAUDE.md`, [#3](https://github.com/DavidMS/biwenger-manager/issues/3) settings and hooks, [#4](https://github.com/DavidMS/biwenger-manager/issues/4) skills, [#5](https://github.com/DavidMS/biwenger-manager/issues/5) agents, [#6](https://github.com/DavidMS/biwenger-manager/issues/6) this documentation and [#7](https://github.com/DavidMS/biwenger-manager/issues/7) GitHub templates.

## Goal

Claude Code forgets everything between sessions. Without setup, each session would have to rediscover the project, would ask permission for every `git status`, and would rely on good intentions to avoid pushing to `main` or reading the credentials file. Phase 0 turns those intentions into files the tool loads and enforces.

## The pieces

```
CLAUDE.md                    always loaded: what the project is and the rules
.claude/settings.json        what Claude may run without asking, what it may never read, hooks
.claude/hooks/               scripts that run before or after tool calls
.claude/skills/<name>/       know-how loaded only when the task needs it
.claude/agents/<name>.md     specialised helpers with their own context and limited tools
```

They differ in **when they load** and **whether they are enforced**:

| Piece | Loads | Enforced? |
|---|---|---|
| `CLAUDE.md` | Every session, always | No, it's instructions |
| Permissions | Every tool call | Yes |
| Hooks | Every matching tool call | Yes, a script can block |
| Skills | When the task matches the skill's description, or `/skill-name` | No, it's instructions |
| Agents | When the main session delegates to them | Their tool list and hooks are |

The rule of thumb that came out of this phase: **put rules where they are enforced when you can, and in instructions when you can't.**

### `CLAUDE.md` — project memory (#2)

The root [`CLAUDE.md`](../../CLAUDE.md) is loaded into every session. It holds a one-paragraph description, the repo map, the commands, eight golden rules and the issue → branch → PR workflow. It's about 60 lines on purpose: everything in it costs context in every session.

Two choices worth copying:

- **Describe the repo as it is.** Folders and commands that don't exist yet are marked *(planned)*, so Claude doesn't try to run them.
- **Rules, not essays.** The reasons live in the plan and in ADRs, which are read only when needed.

### Settings: permissions and hooks (#3)

[`.claude/settings.json`](../../.claude/settings.json) is shared through git.

- **Allow list.** Commands that are safe and frequent (`./mvnw`, `npm test`, `docker compose`, read-only `gh`, everyday `git`) run without a prompt. `git push` is deliberately *not* on it: pushing sends work off the machine, so the human approves each one.
- **Deny list.** `Read` is denied for `.env` files and `secrets/`, where Biwenger credentials live.
- **Hooks.** A deny rule on `Read` doesn't stop `cat .env` in a shell, so a `PreToolUse` hook, [`guard-bash.sh`](../../.claude/hooks/guard-bash.sh), checks every Bash command and blocks force-pushes to `main` and commands that print `.env`. A `PostToolUse` hook, [`format-file.sh`](../../.claude/hooks/format-file.sh), formats each edited file (Spotless for Java, Prettier for the frontend) and does nothing until those tools exist in Phase 1.

Personal overrides go in `.claude/settings.local.json`, which is gitignored.

### Skills — on-demand know-how (#4)

A skill is a folder with a `SKILL.md`: a `description` in the frontmatter, then instructions. Claude sees only the description until a task matches it, then loads the rest. That keeps long reference material out of every session.

| Skill | Use |
|---|---|
| `implement-issue` | The whole loop for one issue. Run `/implement-issue 12` |
| `backend-feature`, `frontend-feature` | Order of work and test types for each side (provisional until Phase 1) |
| `biwenger-api` | Endpoints, headers, auth flow, game rules, how to capture a new endpoint |
| `write-adr` | Template and numbering for [ADRs](../adr/) |
| `tutorial-chapter` | Structure and tone for these chapters and the journal |

The `description` is the most important line: it decides whether the skill is used at all. Each one says what it covers *and* when to use it.

### Agents — specialists with limited tools (#5)

A subagent runs in its own context, so a long review doesn't fill the main session, and it gets only the tools its job needs.

| Agent | Job | Limits |
|---|---|---|
| `java-reviewer`, `react-reviewer` | Review a diff and report findings | Can't edit. Can run tests and lint |
| `test-writer` | Write missing tests | Can edit test files only |
| `biwenger-api-explorer` | Research the Biwenger API, document endpoints | GET requests only. Writes only under `docs/biwenger-api/` |
| `tutorial-scribe` | Write the journal entry for an issue | Writes only under `docs/tutorial/` |

An agent's `tools` list can only switch whole tools on or off. "Bash, but only Maven" or "Edit, but only test files" can't be expressed there. So each agent's frontmatter adds a `PreToolUse` hook that calls [`agent-guard.py`](../../.claude/hooks/agent-guard.py) with that agent's profile: an allow list of commands and the paths it may edit. Everything else is blocked.

## How to verify it works

Don't trust a configuration you haven't seen fire. Each piece was checked like this, and you can repeat the checks:

| Piece | Check |
|---|---|
| `CLAUDE.md` | Start a new session and ask "what are the golden rules of this project?" |
| Permissions | `/permissions` lists the rules. `git status` runs without a prompt; `git push` asks |
| Bash guard | Pipe sample input into it: `echo '{"tool_input":{"command":"cat .env"}}' \| bash .claude/hooks/guard-bash.sh; echo $?` prints the block message and `2` |
| Hooks in a session | `/hooks` lists them. Ask Claude to `cat .env`: the guard blocks it |
| Skills | `claude -p "List the project skills available to you"` in a fresh session. Then ask a question only one skill can answer |
| Agents | `/agents` lists them. Ask for a review: "use java-reviewer on …". Ask an agent to do something outside its limits and watch the guard block it |

## What went wrong

Each issue's own verification caught real bugs before review:

- **The `.env` guard blocked a harmless command and missed a harmful one** ([#3](journal/0003-claude-settings.md)). `cp .env.example .env` was blocked, while `cat .env .env.example` got through. Found by pipe-testing 18 cases.
- **A skill argument expanded too much** ([#4](journal/0004-project-skills.md)). `$ARGUMENTS` is the whole text after the command, so `/implement-issue 5 Do not execute anything` produced `gh issue view 5 Do not execute anything`. Found by running the skill in a fresh session; fixed with `$0`.
- **The API explorer could send a POST** ([#5](journal/0005-project-agents.md)). A regex consumed the space before `-X`, so `curl -X POST` passed the GET-only check. Found by pipe-testing.

None of these would have shown up by reading the files. All of them showed up by running them.

## Results

- 1 `CLAUDE.md`, 1 settings file, 3 hook scripts, 6 skills and 5 agents.
- Every piece verified in a live or fresh session, with the results in each PR.
- Prompts shrank to one line: "done, PR is merged. start #4".

## Takeaways

- Keep `CLAUDE.md` short and true today. Move long know-how into skills.
- Use permissions and hooks for anything that must never happen. Instructions are requests; a hook that exits with code 2 is a rule.
- Give agents the least access their job needs, and enforce it with a hook when the tool list is too coarse.
- Test your AI configuration like code: pipe in sample inputs, then prove it fires in a real session.
- Write the description of a skill or agent for the model that has to decide whether to use it.
