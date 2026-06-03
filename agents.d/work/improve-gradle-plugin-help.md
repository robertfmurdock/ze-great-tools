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
- [x] Research Gradle plugin help best practices
  - Agent cycle: test → implement → refactor-light → verify pushable
  - How do other well-documented plugins surface help?
  - Options: custom help tasks, improved README placement, runtime diagnostics, configuration validation with helpful messages
  - Document feasible approaches for Gradle context
  - Completed: Identified hybrid approach with 3 phases
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
- Tagger plugin: tasks `calculateVersion`, `release`, `tag`, `commitReport`, `previousVersion`, `githubRelease`
  - `calculateVersion` description: "Read-only: calculate next version from commit history without tagging. Check snapshot == false before tagging."
  - Group: "versioning" (well-organized)
  - All tasks have clear descriptions indicating read-only vs side-effect operations
- Digger plugin: tasks `currentContributionData`, `allContributionData`, `gitHead`
  - Tasks have no descriptions (CurrentContributionData) or minimal group assignment
  - Group assignment missing on main tasks
- No runtime help tasks or enhanced help output
- Standard Gradle `help --task <taskname>` provides only task path, type, basic description
- Tagger plugin descriptions already follow good pattern: operation type prefix (Read-only, Side effect, Orchestrator) + purpose

### Gradle Plugin Help Best Practices (Research Completed 2026-06-03)

**Official Gradle documentation guidance (docs.gradle.org):**
- Tasks SHOULD include both `group` and `description` properties
  - Groups categorize tasks; descriptions explain what a task does
  - Example: `group = "Application"; description = "Runs this project as a JVM application"`
  - Tasks without groups are "hidden" from `gradle tasks` but remain executable
  - Shown via `gradle tasks --all`
- Command-line options using `@Option` annotations are self-documenting
  - `gradle help --task <taskname>` automatically shows options and descriptions
  - Options rendered alphabetically with descriptions from annotations
- No official guidance on dedicated "help tasks" or "guide tasks" for plugins

**Gradle Plugin Portal requirements (plugins.gradle.org):**
- Must have clear `description` field specifying plugin intent
- Must set `tags` for categorization and searchability
- Project URL should point to documentation (in English) or sources
- Documentation must be publicly accessible (broken links = rejection)
- Plugins must provide meaningful functionality (not "hello world")

**Native Gradle help mechanisms:**
- `gradle tasks [--group <name>]`: Lists available tasks with descriptions
- `gradle help --task <taskname>`: Shows detailed task info (path, type, options, description, group)
- Task descriptions and groups are primary discoverability mechanism
- No standard pattern for "guide" or "philosophy" content in Gradle ecosystem

**Feasible help approaches for Gradle plugins:**
1. **Enhanced task descriptions**: Improve existing task metadata (group, description)
   - Pros: Zero runtime overhead, appears in standard `gradle tasks` output, no new APIs
   - Cons: Limited formatting, no space for examples or workflows, restricted to ~120 chars
   - Current state: Tagger already uses descriptive prefixes effectively
2. **Custom help tasks**: Add dedicated `taggerHelp` / `diggerHelp` tasks that print formatted help
   - Pros: Can include examples, box drawings, formatted output like CLI; progressive disclosure pattern
   - Cons: Not discoverable unless task list is checked; requires maintenance; clutters task namespace
   - Pattern: Similar to CLI's `guide` subcommand approach
3. **Task execution messages**: Add `doFirst` blocks that print helpful context
   - Pros: Contextual help when running tasks; can warn about prerequisites
   - Cons: Only visible during execution, not during discovery phase
4. **Configuration validation with helpful errors**: Validate extension configuration and provide actionable messages
   - Pros: Catches misconfigurations early; teaches correct usage
   - Cons: Only fires on misconfiguration, not useful for discovery

**Industry patterns observed:**
- Most Gradle plugins rely on README documentation for comprehensive guides
- Task descriptions are kept terse and focused on "what" not "how" or "why"
- Error messages during task execution provide tactical guidance
- No widespread pattern for in-terminal "guide" content (unlike CLI tools)

**Recommended hybrid approach:**
- **Phase 1 (quick wins)**: Enhance existing task descriptions and groups
  - Digger plugin: Add groups and descriptions matching tagger quality
  - Both plugins: Ensure descriptions appear in `gradle tasks` output
- **Phase 2 (discovery)**: Add dedicated help tasks
  - `taggerGuide` task with fit assessment, examples, workflow guidance
  - `diggerGuide` task with prerequisites, best practices, regex contracts
  - Keep tasks lightweight (just formatted println), no external dependencies
- **Phase 3 (validation)**: Add doFirst diagnostics for common misconfigurations
  - Tagger: warn if not on release branch, if detached head without flag
  - Digger: warn about missing git history, empty repositories

### Success Criteria
- Users can discover plugin capabilities without leaving terminal
- Examples and common workflows are easily accessible via dedicated guide tasks
- Help quality comparable to CLI experience (considering Gradle context differences)
- Standard `gradle tasks` output shows well-organized, descriptive task list

## Validation
- Commands:
  - `./gradlew :tools:tagger-plugin:check`
  - `./gradlew :tools:digger-plugin:check`
  - `./gradlew check`
  - Test help tasks (TBD based on implementation)
- Results: (to be filled in during implementation)
