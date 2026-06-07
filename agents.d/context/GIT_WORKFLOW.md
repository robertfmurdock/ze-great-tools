---
load_when: creating commits, pull requests, or analyzing git history
cost: ~350 tokens
brief: git workflow practices, commit standards
---

# Git Workflow

## Purpose
Standards for git operations: commits, history analysis.

## When To Use
- Creating commits
- Analyzing git history
- resolving conflicts

## Critical Facts

### Commit Creation
- NEVER skip hooks, update git config, or force push to main/master
- NEVER push commits when operating in interactive mode — let user push manually
- Always create NEW commits (never amend unless explicitly requested)
- When pre-commit hook fails, commit did NOT happen — fix and create NEW commit
- Stage specific files by name (avoid `git add -A` or `git add .`)
- NEVER commit without explicit user request
- Exclude secrets: `.env`, `credentials.json`, etc.

### Commit Message Format
**MANDATORY TEMPLATE** — Fill in, do not skip semver marker:
```
[<semver>] <one-line summary>

<optional 1-2 sentence details>

Co-Authored-By: <Agent Name> <noreply@<agent-provider>.com>
```

Semver marker (REQUIRED first character):
- `[major]`: breaking change
- `[minor]`: new backward-compatible functionality
- `[patch]`: bug fix, refactor, build output changes
- `[none]`: docs, work cards, build config (no output impact)

Guidelines:
- Check work card for semver intent BEFORE drafting message
- Concise summary (why, not what)
- Accurate verbs: `add` = new feature, `update` = enhancement, `fix` = bug fix
- Check `git log` for repository style
- Always end with: `Co-Authored-By: <Agent Name> <noreply@<agent-provider>.com>`
- Use HEREDOC:
```bash
git commit -m "$(cat <<'EOF'
[none] Add semver marker here.

Details here.

Co-Authored-By: <Agent Name> <noreply@<agent-provider>.com>
EOF
)"
```

### Commit Workflow
1. Parallel: `git status` (never `-uall`), `git diff`, `git log`
2. Draft message
3. Stage files, commit, verify with `git status`
4. If hook fails: fix and create NEW commit

### History Analysis
- List commits: `git log --oneline <range>`
- Modified files: `git diff <range> --name-only`
- Use exact SHAs

## Constraints
- **NEVER `git push` in interactive mode** — user pushes manually
- No push unless explicitly requested (automation/CI context only)
- No `-i` flag (interactive not supported)
- No `--no-edit` with `git rebase`
- No empty commits
- Quote paths with spaces
- Avoid prepending `cd` to git commands

## Key Files
- `.github/workflows/*.yml` — CI/CD workflows
- `agents.d/work/` — work cards defining commit scope

## Decisions
- NEW commits over amending (preserves work)
- Specific file staging over bulk add (avoids secrets)
- HEREDOC for messages (proper formatting)
- `gh` CLI for GitHub operations

## Common Mistakes
- **Forgetting semver marker at start of commit message**
- **Pushing commits in interactive mode (user must push)**
- `git add -A` committing sensitive files
- Amending after failed hook (destroys previous commit)
- Skipping hooks without permission
- Force pushing without request
- Forgetting co-author line
- Using `-uall` with `git status`
- Committing without user request
