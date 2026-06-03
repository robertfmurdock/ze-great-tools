# Document CLI Execution Patterns

## Goal
Create a context document (`agents.d/context/CLI_EXECUTION.md`) that provides clear guidance for agents on how to execute one-off runs of the CLI tools in this project (tagger-cli, digger-cli) without flailing.

## Constraints
- Must be token-optimized agent-facing markdown (not human-facing)
- Should prevent common failure patterns (wrong working directory, missing build steps, incorrect arguments)
- Must integrate with existing context/index.md structure
- Document must be informational/research-focused, not prescriptive until patterns are validated
- Semver: N/A (documentation only)
- Must use `./gradlew` for all automation
- Must run `./gradlew check` before completion

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Research current CLI build and execution patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - How are tagger-cli and digger-cli currently built?
  - What Gradle tasks produce runnable artifacts?
  - Where do built artifacts live (build/install, build/bin, etc)?
  - What are the working directory requirements for each CLI?
- [ ] Identify common execution failure modes
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Ask: What happens if CLI is run from wrong directory?
  - Ask: Do CLIs require specific environment variables or config files?
  - Ask: How do CLIs handle missing dependencies or git state?
  - Document error patterns that indicate execution environment issues
- [ ] Document verified execution patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test execution via `./gradlew :command-line-tools:tagger-cli:run --args="..."`
  - Test execution via `./gradlew installDist` then running scripts
  - Test execution via native binaries (if applicable)
  - Document which method is preferred for one-off testing vs production use
  - Verify each pattern works from project root
- [ ] Create CLI_EXECUTION.md context document
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Location: `agents.d/context/CLI_EXECUTION.md`
  - Structure: token-optimized (no human prose, bullet format)
  - Include: build commands, execution patterns, working directory requirements, common pitfalls
  - Include: how to pass arguments for typical use cases (help, version, actual operations)
- [ ] Update agents.d/context/index.md to reference new document
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add appropriate reference/trigger for when CLI execution guidance is needed
  - Ensure document is loaded when work involves CLI testing or execution
- [ ] Validate document by executing both CLIs using documented patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Follow CLI_EXECUTION.md exactly to run tagger-cli --help
  - Follow CLI_EXECUTION.md exactly to run digger-cli --help
  - Verify instructions are complete and unambiguous
  - Test from both project root and module directories if relevant
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
### Discovery Questions
These questions should be answered during research phase to avoid flailing:

1. **Build Process**
   - What Gradle tasks build the CLIs?
   - Are native binaries built, or only JVM artifacts?
   - Do CLIs use multiplatform (commonMain suggests yes)?

2. **Execution Methods**
   - `./gradlew :module:run --args`?
   - `./gradlew installDist` then run from `build/install/`?
   - Direct execution of native binaries?
   - Which method is most reliable for testing changes?

3. **Environment Requirements**
   - Do CLIs expect to run from project root or can run anywhere?
   - Do they need git repository state?
   - Any required config files or environment variables?

4. **Argument Patterns**
   - How to pass `--help` via Gradle run task?
   - How to pass multi-word arguments or flags?
   - Escaping requirements for shell vs Gradle?

5. **Integration with Gradle**
   - Is there a preferred task for testing CLI changes?
   - How do plugin versions relate to CLI versions?
   - Should agents prefer Gradle plugins over direct CLI execution when possible?

### Target Audience
Agents performing:
- Quick CLI smoke tests during development
- Validation of CLI changes before commit
- Debugging CLI behavior
- Comparison of CLI vs Gradle plugin behavior

### Success Criteria
- Agent can execute either CLI with `--help` on first attempt without errors
- Agent can execute either CLI with realistic arguments
- Document prevents "command not found", "wrong directory", "gradle task not found" failures
- Clear guidance on when to rebuild vs reuse existing builds

## Validation
- Commands:
  - `./gradlew check` (ensure no regressions)
  - Follow documented patterns to execute tagger-cli --help
  - Follow documented patterns to execute digger-cli --help
- Results: (to be filled in during implementation)
