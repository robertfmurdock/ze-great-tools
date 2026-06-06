# Create Working Examples Directory

## Goal
Create `examples/` directory with complete, working examples demonstrating tagger and digger usage in real-world scenarios to help users evaluate fit and get started quickly.

## Constraints
- Examples must be minimal, focused, and actually runnable
- Each example includes README explaining scenario and expected outcomes
- Examples must follow current best practices from tool READMs
- Semver intent: `[none]` - documentation/examples only, no tool code changes
- Must work with published versions of tools (not require local build)
- All example READMs must be verified using DOCUMENTATION.md protocol

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Create `examples/README.md` with overview of all examples and how to use them
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create `examples/gradle-simple/` - minimal Gradle project using tagger plugin
    - build.gradle.kts with tagger plugin
    - .tagger configuration file
    - README explaining setup and basic usage
    - Example commits demonstrating [patch], [minor], [major]
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create `examples/npm-package/` - minimal NPM package using tagger CLI
    - package.json
    - .tagger configuration
    - README with installation and usage
    - Example GitHub Actions workflow (optional, in comments)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create `examples/github-actions/` - example GitHub Actions workflows
    - `workflows/semantic-versioning.yml` - complete tagger workflow
    - `workflows/contribution-analytics.yml` - digger workflow
    - README explaining each workflow
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Test each example by following its README from scratch (verify examples actually work)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify all links in example READMs work (see DOCUMENTATION.md)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Run grammar and formatting checks on all example markdown files
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: (to be filled on start)
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to start
- **Date**: 2026-06-06

## Implementation Notes
_(newest first)_

### 2026-06-06: Work card created
Semver intent: `[none]` - examples and documentation only, no tool code changes.

Examples should:
- Use published versions (latest from Gradle Plugin Portal / NPM)
- Be copy-pasteable for quick starts
- Include realistic commit messages
- Show expected output

Consider adding `.gitignore` to examples to exclude build artifacts if they're meant to be run.

Each example should be self-contained (can be copied out and used independently).

Reference material:
- Tagger README: tools/tagger-plugin/README.md
- Tagger CLI README: command-line-tools/tagger-cli/README.md
- Digger README: tools/digger-plugin/README.md

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass (no impact expected)
- [ ] Follow `examples/gradle-simple/README.md` instructions in a temporary directory and verify they work
- [ ] Follow `examples/npm-package/README.md` instructions in a temporary directory and verify they work
- [ ] Verify `examples/github-actions/` workflows are syntactically valid YAML
- [ ] Verify all links in example READMs work (see DOCUMENTATION.md)
- [ ] Run grammar check on all example markdown: `mcp__idea__get_file_problems` for each README
