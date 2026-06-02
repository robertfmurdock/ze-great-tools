---
purpose: Bootstrap instructions for AI agents working in this repository
audience: software engineering agents (Claude Code, custom agents)
override: all default behaviors
---

# Agent Instructions

## Context System
- **Index**: `agents.d/context/index.md` — document catalog with load gates and token costs
- **Bootstrap**: Always load `agents.d/context/PERSONA.md` first
- **Task-gated**: Load context documents only when their trigger conditions match current work

## Execution Protocol
- **Build tool**: `./gradlew` for all tasks — no ad hoc shell scripts
- **Automation**: Express repository automation as Gradle tasks
- **Scope**: Start module-scoped, expand only if needed
- **Validation**: Run `./gradlew check` before completing any task

## Documentation Protocol
**Human-facing markdown** (READMEs, guides, planning docs):
- Grammar check: `mcp__idea__get_file_problems`
- Format: `mcp__idea__reformat_file`

**Agent-facing markdown** (`agents.d/` hierarchy, work cards):
- Do NOT apply IDEA formatting — token-optimized structure takes precedence
- Maintain existing format conventions
