# Tagger Gradle Plugin Flag Improvements

## Goal
Propagate CLI flag improvements (--allow-detached-head) to tagger-plugin and digger-plugin for consistent behavior across CLI and Gradle tooling.

## Constraints
- Maintain backwards compatibility (deprecated flags work for one release)
- Task behavior must be consistent with CLI commands
- Follow PERSONA, code style playbook, and Gradle playbook
- Changes must comply with TestMints patterns and verification requirements from `.junie/guidelines.md`

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Add allowDetachedHead property to TaggerExtension with proper deprecation of disableDetached
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Update CalculateVersion task to support both old and new flags with proper fallback logic
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update TagVersion task to add allowDetachedHead parameter
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Wire tasks properly in TaggerPlugin to handle optional properties correctly
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify all functional tests pass, especially backwards compatibility tests
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Consider whether digger-plugin needs similar updates
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Context from CLI Work
The CLI now has:
- `--allow-detached-head` flag (default: false) on both `calculate-version` and `tag` commands
- Old `--disable-detached` flag is hidden but still works for backwards compatibility
- Clear positive framing: `--allow-detached-head=true` means "allow detached HEAD"

### Plugin Requirements
- TaggerExtension should expose `allowDetachedHead` property (no convention, nullable)
- Keep `disableDetached` property with `@Deprecated` annotation for backwards compatibility
- Tasks (CalculateVersion, TagVersion) need to support both properties
- Priority: if `allowDetachedHead` is explicitly set, use `!allowDetachedHead`; otherwise use `disableDetached`
- Challenge: Gradle Property with `.convention()` makes detecting "not set" difficult - need to handle optional properties correctly

### Key Technical Issues Discovered
- Gradle Property.isPresent doesn't work as expected when convention is set
- Need to carefully handle the transition: user might set old property, new property, or neither
- Task registration needs conditional `.set()` calls to avoid forcing presence
- Functional tests use old property names and must continue to work

### Potential Approaches
1. **No convention on new property**: Make `allowDetachedHead` have no convention (nullable), only set in task if explicitly set in extension
2. **Explicit user tracking**: Add internal flag to track which property user actually set
3. **Convention-based priority**: Set both in tasks, use presence detection carefully

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
