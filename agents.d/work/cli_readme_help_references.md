# CLI README Help References

## Goal
Add direct links from CLI READMEs to extracted help markdown files for improved GitHub discoverability.

## Constraints
- Maintain existing README structure and content
- Links must work on GitHub (relative paths from README location)
- Keep README focused on quick start; links provide depth
- Semver intent (initial): `[none]` — documentation enhancement, no code/behavior change

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] Add help file references to Tagger CLI README
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Link to tagger-guide.md in guide reference section
  - Link to command-specific help files (calculate-version.md, tag.md)
  - Enhance Help section with direct documentation links
- [ ] Add help file references to Digger CLI README
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Link to digger-guide.md in guide reference section
  - Link to command-specific help files (current-contribution-data.md, all-contribution-data.md)
  - Enhance Help section with direct documentation links
- [ ] Verify links work on GitHub
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Check relative paths resolve correctly in repository browser
  - Confirm markdown renders properly
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
**Semver intent (initial)**: `[none]` — documentation-only changes, no code or API impact

**Context**: Following CLI markdown extraction rollout (completed 2026-05-31), help text now exists as linkable markdown files in `src/commonMain/resources/help/`. READMEs currently only reference the CLI's `--help` flag without linking to the actual documentation.

**Link structure**:
- From `command-line-tools/tagger-cli/README.md` → `src/commonMain/resources/help/*.md`
- From `command-line-tools/digger-cli/README.md` → `src/commonMain/resources/help/*.md`

**Improvement areas**:
1. **Tagger CLI** (README.md lines 418-425):
   - Current: Generic "use built-in help" text
   - Add: Links to tagger-guide.md, calculate-version.md, tag.md, tagger.md
   - Consider: Inline preview of key help sections (snapshot reasons table, workflow)

2. **Digger CLI** (README.md lines 203-215):
   - Current: "digger guide" command reference only
   - Add: Direct link to digger-guide.md
   - Add: Links to command-specific help (current-contribution-data.md, all-contribution-data.md)

**Link examples**:
```markdown
For complete documentation, see:
- [Tagger Guide](src/commonMain/resources/help/tagger-guide.md) - Fit assessment and philosophy
- [Calculate Version Help](src/commonMain/resources/help/calculate-version.md) - Snapshot reasons and remediation
- [Tag Help](src/commonMain/resources/help/tag.md) - Workflow and version override guidance
```

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
