# Task: Improve tagger error handling and messages

## Goal
Fix error messages in tagger-cli to be more actionable and user-friendly, eliminating friction for CI/CD users.

## Background
Issue #310 identified multiple error message issues that cause confusion and wasted debugging cycles:
1. Git push failures surface as Kotlin/JS stack traces instead of actionable permission errors
2. "No tags" message doesn't distinguish between truly empty repos and repos with only lightweight tags

Both issues cause users to spend time diagnosing problems that should be immediately clear from the error message.

## Hard constraints
- Maintain backwards compatibility for error exit codes
- Don't break existing error parsing in downstream tools
- Preserve helpful context while making messages more actionable

## What to change

### 1. Git push failure handling (Issue #310 item #1)

**Current behavior:**
```
/ADOagent/_work/.../node_modules/git-semver-tagger/kotlin/clikt-clikt.js:2111
        throw $p;
        ^\nException: remote: 001f# service=git-receive-pack
remote: 0000000000aaTF401027: You need the Git 'GenericContribute' permission ...
fatal: unable to access '<url>': The requested URL returned error: 403
    at runProcess_0 (.../tools-git-adapter.js:384:13)
    ...
```

**Target behavior:**
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

**Implementation:**
- In `tools-git-adapter` (likely `runProcess` function), catch non-zero exit from `git push`
- Extract stderr content from the git command
- Format as user-facing error with context and remediation steps
- Suppress Kotlin/JS stack traces unless `DEBUG=tagger` or similar env var is set
- Update tag command to handle this error gracefully

### 2. Lightweight vs annotated tags detection (Issue #310 item #3)

**Current behavior:**
```
Welcome to Tagger CLI.
Inappropriate configuration: repository has no tags.
If this is a new repository, use `tag` to set the initial version.
```
(When lightweight tags actually exist)

**Target behavior:**
```
Inappropriate configuration: found 1 tag (0.0.0) but it is lightweight.
Tagger requires annotated tags so it can record version metadata.

Recreate it with:
  git tag -d 0.0.0
  git tag -a 0.0.0 <sha> -m "Initial version"
  git push --force origin 0.0.0
```

**Implementation:**
- In version calculation logic (likely `tools-tagger-core`), detect both lightweight and annotated tags
- When only lightweight tags exist, provide specific error message
- Include exact commands to fix the issue
- Update error message formatting to match pattern: problem → explanation → solution

### 3. Documentation updates

- Update README with section on common error messages
- Document the lightweight vs annotated tag requirement prominently
- Add troubleshooting section covering permission errors

## Checklist

### Code changes
- [ ] Add git push error handling in `tools-git-adapter` `runProcess` function
- [ ] Format git errors with stderr content + remediation guidance
- [ ] Add DEBUG mode check to suppress/show JS stack traces
- [ ] Add lightweight tag detection in version calculation
- [ ] Create specific error message for lightweight-only repos
- [ ] Update error message formatting to be consistent

### Testing
- [ ] Write test for git push failure (non-zero exit)
- [ ] Verify stack trace is suppressed in normal mode
- [ ] Verify stack trace shows in DEBUG mode
- [ ] Write test for lightweight tag detection
- [ ] Write test for annotated tag detection (ensure no regression)
- [ ] Test error message formatting matches expected output
- [ ] Run `./gradlew check` and verify all tests pass

### Documentation
- [ ] Update README with "Common Errors" section
- [ ] Document lightweight vs annotated tag requirement
- [ ] Add troubleshooting guide for permission errors
- [ ] Update help text if needed

### Validation
- [ ] Test with actual git push failure (403/permission denied)
- [ ] Test with repo containing only lightweight tags
- [ ] Verify error messages are clear and actionable
- [ ] Move this file to `agents.d/work_completed/`

## Definition of done
- Users hitting git push failures see actionable error messages with specific remediation steps, not Kotlin/JS stack traces
- Users with lightweight tags see a clear explanation of the issue and exact commands to fix it
- All error messages follow consistent format: problem → explanation → solution
- Tests cover both error cases and verify message content
- Documentation reflects the improved error handling
