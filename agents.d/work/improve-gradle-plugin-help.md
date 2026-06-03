# Improve Digger and Tagger Gradle Plugin Help Quality

## Goal
Enhance the digger-plugin and tagger-plugin Gradle plugins to provide the same level of help quality and user experience as their respective CLI tools.

## Constraints
- CLI tools provide comprehensive, well-formatted `--help` output with examples and guides
- Gradle plugins currently rely solely on README documentation
- Must add runtime help commands or improve discoverability without breaking existing APIs
- Semver: `[minor]` — adding new help features without breaking existing functionality
- Must use `./gradlew` for all automation
- Must run `./gradlew check` before completion
- All checklist items must result in pushable, non-failing state

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [x] Analyze CLI help output quality and features
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Document tagger CLI help features (structured help, examples, guide command)
  - Document digger CLI help features
  - Identify what makes CLI help effective (formatting, examples, workflow guidance)
- [ ] Research Gradle plugin help best practices
  - Agent cycle: test → implement → refactor-light → verify pushable
  - How do other well-documented plugins surface help?
  - Options: custom help tasks, improved README placement, runtime diagnostics, configuration validation with helpful messages
  - Document feasible approaches for Gradle context
- [ ] Design help improvement strategy for both plugins
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Choose approach: dedicated help tasks, enhanced error messages, or both
  - Ensure consistency between tagger-plugin and digger-plugin
  - Define what "help quality" means in Gradle context
- [ ] Implement help improvements for tagger-plugin
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add help task or improve existing task output
  - Include examples, common workflows, and configuration guidance
  - Test help output is clear and actionable
- [ ] Implement help improvements for digger-plugin
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add help task or improve existing task output
  - Include examples, common workflows, and configuration guidance
  - Test help output is clear and actionable
- [ ] Update plugin READMEs to reference new help features
  - Agent cycle: test → implement → refactor-light → verify pushable
  - tools/tagger-plugin/README.md
  - tools/digger-plugin/README.md
  - Document how to access help from Gradle
- [ ] Verify help output quality meets CLI standard
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Compare plugin help to CLI help
  - Ensure examples are present and relevant
  - Confirm workflow guidance is clear
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
### CLI Help Analysis (Completed 2026-06-03)

**Tagger CLI Help Quality:**
- Main help: Comprehensive usage summary, typical CI usage example in box drawing, format options, clear command list
- Guide command: Dedicated fit assessment with "Use when"/"Don't use when" criteria, best practices (Do/Don't format), workflow philosophy, external docs link
- Well-formatted with box drawing characters, tables, clear sections
- Examples embedded directly in help output
- Philosophy and tradeoffs documented inline

**Digger CLI Help Quality:**
- Main help: Usage summary with example commands in box, command reference table with purposes
- Guide command: Fit assessment, prerequisites, first-run workflow (numbered steps), best practices, regex override contract details
- Similar formatting quality to tagger (box drawings, clear sections)
- Inline examples and usage patterns
- Comprehensive prerequisites and workflow guidance

**What Makes CLI Help Effective:**
- Structured visual formatting (box drawings, tables, sections with clear headers)
- Concrete usage examples embedded in help (not just "see docs")
- Fit assessment: explicit "use when" and "don't use when" criteria
- Philosophy/workflow guidance: helps users understand tool intent
- Progressive disclosure: main help is concise, guide provides depth
- Best practices in actionable Do/Don't format
- Links to external docs for deep-dive

### Current State (Gradle Plugins)
- Tagger plugin: tasks `calculateVersion`, `release`
  - `calculateVersion` description: "Read-only: calculate next version from commit history without tagging. Check snapshot == false before tagging."
  - Group: "versioning"
- Digger plugin: tasks `currentContributionData`, `allContributionData`
  - `currentContributionData` description: "-" (empty)
  - Group: "-" (unassigned)
- No runtime help tasks or enhanced help output
- Standard Gradle `help --task <taskname>` provides only task path, type, basic description

### Design Options
1. **Custom Help Tasks**: Add dedicated `taggerHelp` / `diggerHelp` tasks with formatted output
2. **Enhanced Task Descriptions**: Improve built-in task descriptions and doFirst diagnostic output
3. **Configuration Validation**: Add helpful validation messages during configuration phase
4. **Hybrid**: Combine multiple approaches for comprehensive help experience

### Success Criteria
- Users can discover plugin capabilities without leaving terminal
- Examples and common workflows are easily accessible
- Help quality comparable to CLI experience (considering Gradle context differences)

## Validation
- Commands:
  - `./gradlew :tools:tagger-plugin:check`
  - `./gradlew :tools:digger-plugin:check`
  - `./gradlew check`
  - Test help tasks (TBD based on implementation)
- Results: (to be filled in during implementation)
