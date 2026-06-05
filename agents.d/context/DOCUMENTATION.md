---
load_when: creating/modifying user-facing documentation (README, guides, markdown docs)
cost: ~400 tokens
brief: user-facing docs standards, link verification, grammar/formatting
---

# Documentation Standards

## Purpose
Standards for user-facing documentation quality, verification, link integrity.

## When To Use
Creating/modifying README files, guides, tutorials, or any markdown documentation intended for end users.

## Critical Facts

### User-Facing Documentation Requirements
All user-facing documentation (README.md, guides, tutorials, etc.) MUST:
1. **Link verification**: All links must be verified to work
   - HTTP/HTTPS URLs: accessible and return non-error status
   - Relative file paths: file exists in repository
   - Anchor links: target heading/section exists
   - External references: resource is available
2. **Grammar check**: Run `mcp__idea__get_file_problems` to verify grammar
3. **Format**: Run `mcp__idea__reformat_file` for consistent formatting

### Link Verification Protocol
Before completing documentation work:
1. Extract all links from modified files
2. Verify each link:
   - HTTP/HTTPS: `curl -sL -w "%{http_code}" -o /dev/null <url>` should return 2xx/3xx
   - Relative paths: verify file exists at specified path
   - Anchors: verify target section exists in linked file
3. Fix or remove broken links
4. Document verification in Implementation Notes

### Agent-Facing Documentation
Documents in `agents.d/` hierarchy and work cards are agent-facing:
- Do NOT apply IDEA formatting (token-optimized structure takes precedence)
- Do NOT require link verification (links may reference future work)
- Maintain existing format conventions

## Constraints
- Never merge user-facing documentation with broken links
- Verification applies to NEW and MODIFIED links
- Pre-existing broken links should be fixed when encountered
- Link verification is part of work validation, not optional

## Key Files
- `WORK_CHECKLIST.md` — when to load documentation standards
- `REFACTOR_AGENT.md` — documentation checks during final refactor

## Common Mistakes
- Skipping link verification "because it looks right"
- Not testing anchor links within same file
- Assuming external URLs still work without verification
- Applying user-facing standards to agent-facing docs
- Not documenting link verification results in Implementation Notes
