# Remove All Deprecated API Surface - Major Version

## Goal
Remove all deprecated APIs from the codebase as part of a major version release, cleaning technical debt and simplifying the public API surface.

## Constraints
- This is a breaking change requiring major version bump (`[major]`)
- Must audit entire codebase for `@Deprecated` annotations
- Must verify no internal usage of deprecated APIs before removal
- Must update all tests that reference deprecated APIs
- Migration guide must document all removed APIs with their replacements
- Must use `./gradlew` for all automation
- Must run `./gradlew check` before completion
- All checklist items must result in pushable, non-failing state
- Separate from npm package migration work (different card)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Audit codebase for all deprecated APIs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Search for all `@Deprecated` annotations across modules
  - Document each deprecated API with its replacement and removal impact
  - Create removal plan organized by module
  - Completed 2026-06-02: Found exactly 2 deprecated APIs (both documented below)
- [x] Remove deprecated `disableDetached` from all locations
  - Agent cycle: test → implement → refactor-light → verify pushable
  - ✅ Remove from TaggerConfig (JSON model) and runtimeDefaultConfig
  - ✅ Remove from TaggerExtension (property + resolveAllowDetachedHead logic)
  - ✅ Remove from CalculateVersion task (property + resolution + deprecation warnings)
  - ✅ Remove from TaggerPlugin (wiring line)
  - ✅ Remove from CLI CalculateVersion (--disable-detached flag + logic)
  - ✅ Update all test files referencing `disableDetached`
  - ✅ Verify all tests pass with only `allowDetachedHead` API
  - ✅ Run ./gradlew check - passed
  - Completed 2026-06-02
- [ ] Update documentation to remove references to deprecated APIs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Remove `disableDetached` from configuration examples
  - Update migration guides if needed
  - Ensure all docs reference current APIs only
- [ ] Create migration guide for major version
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Document all removed APIs
  - Provide replacement patterns for each removal
  - Include version compatibility matrix
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
### Known Deprecated APIs (as of 2026-06-02)
**TaggerConfig (JSON model):**
- `disableDetached: Boolean?` in TaggerConfig.kt
- Used in .tagger file config
- Also in runtimeDefaultConfig

**Tagger Plugin - TaggerExtension:**
- `disableDetached: Property<Boolean>` (line ~63 in TaggerExtension.kt)
- Replacement: `allowDetachedHead` (inverted logic)
- Used in file config parsing and resolveAllowDetachedHead()

**Tagger Plugin - CalculateVersion Task:**
- `disableDetached: Property<Boolean>` (line ~40 in CalculateVersion.kt)
- Replacement: `allowDetachedHead` (inverted logic)
- Used in resolveAllowDetachedHead() and buildDeprecationWarnings()

**TaggerPlugin (wiring):**
- Line ~43: task.disableDetached.set(tagger.disableDetached)
- With @Suppress("DEPRECATION")

**CLI - CalculateVersion command:**
- `--disable-detached` hidden CLI flag (line ~44)
- `disableDetachedDeprecated` property
- Used in resolveAllowDetachedHead() and deprecation warnings

### Removal Strategy
1. Start with Gradle plugin APIs (highest impact, most visible)
2. Remove from newest to oldest usage patterns
3. Ensure each removal maintains test coverage via replacement API
4. Document each removal in migration guide before removing

### Testing Focus
- Verify deprecated property tests convert to replacement API tests
- Ensure functional tests cover replacement patterns
- Confirm no internal code paths still use deprecated APIs
- Validate backward compatibility tests are removed appropriately

## Validation
- Commands:
  - `./gradlew check`
  - `./gradlew :tools:tagger-plugin:test`
  - `./gradlew :command-line-tools:check`
- Results: (to be filled during implementation)
