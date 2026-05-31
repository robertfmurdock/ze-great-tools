# Tagger vs Axion Release Plugin Comparison

## Goal

Create a balanced comparison document that helps Gradle/Java teams choose between tagger and Axion Release Plugin.

## Constraints

- Follow PERSONA values: clarity over cleverness, show your work, explicit tradeoffs
- Use existing semantic-release comparison document as structural template
- Maintain balanced, respectful tone (neither tool is universally better)
- Apply CLAUDE.md documentation standards: IDEA grammar checks and formatting before commit
- Semver intent: `[none]` (documentation only, no build output impact)

## Checklist

- [x] Review this work card for compliance with template and update to conform
- [x] Research Axion Release Plugin thoroughly
  - Read official documentation (Read the Docs platform)
  - Review GitHub repository (allegro/axion-release-plugin - 630 stars, v1.21.1 Nov 2025)
  - Identify core features, philosophy, and workflows
  - Find concrete usage examples
  - Understand key differentiators from tagger
- [x] Draft comparison document at `docs/tagger-vs-axion-release.md`
  - Philosophical alignment section (shared values: versions from Git, deterministic, etc.)
  - Quick decision guide (3-4 decision criteria)
  - Feature comparison table
  - Workflow comparison with code examples
  - When to choose tagger (scriptability, platform neutrality, commit-driven versioning)
  - When to choose Axion (Gradle-native, tag-based approach, etc.)
  - Migration considerations
- [x] Update related documentation
  - Add reference in `docs/why-tagger.md` Comparison with Similar Tools section
  - Add callout in `README.md` Gradle plugin section
- [x] Review and finalize
  - Run `mcp__idea__get_file_problems` on all modified markdown files
  - Apply `mcp__idea__reformat_file` on all modified markdown files
  - Verify balance and accuracy
  - Check that examples are concrete and helpful
- [ ] Move to agents.d/work_completed/

## Implementation Notes

**Semver intent (initial):** `[none]` - documentation only, no build output impact

**Research findings:**
- Axion Release Plugin: 630 GitHub stars, latest release Nov 2025 (actively maintained)
- Philosophy: "versions from SCM tags, not hardcoded"
- Comparison angle: Both Gradle plugins, but different version derivation approaches

**Key differentiation angles to explore:**
1. Version derivation: Tagger parses commits for `[major]`/`[minor]`/`[patch]` tokens vs Axion's approach
2. Workflow timing: Tagger separates calculate → tag; Axion may integrate differently
3. Snapshot handling: Compare explicit diagnostics vs Axion's approach
4. Platform scope: Tagger is platform-neutral; Axion is Gradle-first

## Validation

- Commands: 
  - `mcp__idea__get_file_problems` on `docs/tagger-vs-axion-release.md`, `docs/why-tagger.md`, `README.md`
  - `mcp__idea__reformat_file` on `docs/tagger-vs-axion-release.md`, `docs/why-tagger.md`, `README.md`
- Results: 
  - Initial inspection found table formatting issue in new comparison document (resolved by formatter)
  - All modified files now pass grammar inspection (pre-existing anchor warnings in why-tagger.md are not related to this work)
  - Formatting applied successfully to all modified markdown files
