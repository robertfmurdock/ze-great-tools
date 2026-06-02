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
- [ ] Analyze CLI help output quality and features
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
### Current State (CLI)
- Tagger CLI: Comprehensive `--help` with structured commands, examples, and dedicated `guide` command
- Digger CLI: Provides help output (quality TBD - needs investigation)
- Both CLIs use clikt library for well-formatted terminal help

### Current State (Gradle Plugins)
- Tagger plugin: README-only documentation, tasks: `calculateVersion`, `release`
- Digger plugin: README-only documentation, tasks: `currentContributionData`, `allContributionData`
- No runtime help tasks or enhanced help output
- Standard Gradle `help --task <taskname>` provides minimal info

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
