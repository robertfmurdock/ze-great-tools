# Tagger CI Ergonomics and Error Guidance

## Goal
Eliminate multi-iteration CI setup friction by providing context-aware error messages that communicate actual risk and guide users toward proper configuration rather than quick bypasses.

## Constraints
- Must remain backward compatible with existing CLI flags and behavior
- CI detection for error messages only - NO behavior changes based on detected environment
- Error messages must communicate actual risk, not just technical configuration issues
- Error messages must be actionable and platform-specific where relevant
- Critical insight: On release branches, `calculate-version` is as risky as `tag` because version strings trigger release automation
- Follow TestMints patterns for behavioral test coverage
- Semver: [patch] for error message improvements, [minor] for new commands (like `tagger init`)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Escalate "no upstream" error message intensity
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add warning symbols and "RISK:" section to error output
  - Explain actual consequences (unintended production releases on release branches)
  - Detect CI environment and provide platform-specific fix first
  - Show bypass option second with explicit warnings about release branch danger
  - Update plan if guidelines revealed new constraints
- [ ] Enhance docs/tagger-detached-head.md with risk analysis
  - Add "Understanding the Risk" section (why release branches are dangerous)
  - Add "When Safe / When Dangerous" sections with concrete scenarios
  - Add decision matrix table (branch type × command × safety)
  - Emphasize that on release branches, calculate-version is as risky as tag
  - Keep existing content (GitHub Actions fix, ADO guidance)
- [ ] Optional: Add context-aware warning when flag is used on release branch
  - Detect if on release branch when --allow-detached-head is set
  - Emit warning about risk of stable version triggering releases
  - Keep warning concise, reference docs for details
- [ ] Optional: Add `tagger init --ci=<platform>` command for guided setup
  - Detect repository state and ask workflow questions
  - Generate .tagger config with risk-appropriate settings
  - Suggest CI checkout configuration changes
  - Make choices explicit and durable
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Key Discoveries from Analysis

**Critical Insight:** The `allowDetachedHead` flag doesn't suppress snapshot - it just allows calculation when no upstream exists. Then snapshot detection runs but `AHEAD` and `BEHIND` checks can't work (no upstream to compare against).

**Release Branch Risk Parity:** On release branches (main, master), `calculate-version` is just as risky as `tag` because:
- Missing upstream → can't detect AHEAD → might produce stable version (1.2.3) instead of snapshot (1.2.3-SNAPSHOT)
- That stable version triggers release automation (Maven Central, Docker Hub, production deploys)
- The command is read-only, but its output triggers irreversible writes

**Safety by Branch Type:**
- Feature branches: `NOT_RELEASE_BRANCH` adds SNAPSHOT → safe even with flag
- Release branches: No protection if upstream missing → dangerous with flag

**Current Error Too Mild:** "Inappropriate configuration" doesn't convey urgency or explain actual risk (unintended production releases).

**Decision:** Focus on escalating error intensity and enhancing documentation rather than auto-detection that changes behavior. The check exists for good reasons - make those reasons visible and compelling.

## Validation
- Commands:
  - `./gradlew :tools-tests:tagger-core-test:check` - Passed
  - `./gradlew :tools-tests:tagger-test:check` - Passed
  - `./gradlew check` - Passed
- Results: All tests passing, error message properly enhanced with CI detection and risk communication
