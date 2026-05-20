# Tagger Error Handling Improvements

## Goal
Fix error messages in tagger-cli to be more actionable and user-friendly, eliminating friction for CI/CD users.

## Constraints
- Maintain backwards compatibility for error exit codes
- Don't break existing error parsing in downstream tools
- Preserve helpful context while making messages more actionable
- Follow TestMints patterns and test structure rules (see `.junie/guidelines.md`)
- Apply code style from `agents.d/context/PLAYBOOK_CODE_STYLE.md`

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Git push error handling improvement
  - Agent cycle: test → implement → refactor-light → verify pushable
  - **Current behavior:** Kotlin/JS stack traces on git push failures (exit code 128/403)
  - **Target:** Actionable error with stderr content + remediation guidance (Azure DevOps/GitHub permissions)
  - **Scope:** `tools-git-adapter` `runProcess` function; suppress stack traces unless DEBUG env var set
  - Update plan if guidelines revealed new constraints
- [ ] Lightweight tag detection improvement
  - Agent cycle: test → implement → refactor-light → verify pushable
  - **Current behavior:** "repository has no tags" when lightweight tags exist
  - **Target:** Specific error identifying lightweight tags + exact fix commands (delete, recreate annotated, force push)
  - **Scope:** `tools-tagger-core` version calculation logic
  - Update plan if guidelines revealed new constraints
- [ ] Documentation updates
  - Add "Common Errors" section to README
  - Document lightweight vs annotated tag requirement prominently
  - Add troubleshooting section for permission errors
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
**Issue #310 context:**
1. Git push failures surface as Kotlin/JS stack traces instead of actionable permission errors
2. "No tags" message doesn't distinguish between truly empty repos and repos with only lightweight tags

**Target error format pattern:** problem → explanation → solution

**Git push example:**
```
tagger tag: failed to push 0.0.1 to origin (exit code 128)

git stderr:
  remote: TF401027: You need the Git 'GenericContribute' permission...
  fatal: unable to access '<url>': 403

The account running tagger needs push + create-tag permission on the remote.
For Azure DevOps, grant 'Contribute' and 'Create tag' to the Build Service
identity in repo Security settings.
For GitHub Actions, ensure 'permissions: contents: write' on the job.
```

**Lightweight tag example:**
```
Inappropriate configuration: found 1 tag (0.0.0) but it is lightweight.
Tagger requires annotated tags so it can record version metadata.

Recreate it with:
  git tag -d 0.0.0
  git tag -a 0.0.0 <sha> -m "Initial version"
  git push --force origin 0.0.0
```

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
