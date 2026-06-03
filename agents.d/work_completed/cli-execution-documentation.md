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
- [x] Review this work card for compliance with template and update to conform
- [x] Research current CLI build and execution patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - How are tagger-cli and digger-cli currently built?
  - What Gradle tasks produce runnable artifacts?
  - Where do built artifacts live (build/install, build/bin, etc)?
  - What are the working directory requirements for each CLI?
- [x] Identify common execution failure modes
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Ask: What happens if CLI is run from wrong directory?
  - Ask: Do CLIs require specific environment variables or config files?
  - Ask: How do CLIs handle missing dependencies or git state?
  - Document error patterns that indicate execution environment issues
- [x] Document verified execution patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test execution via `./gradlew :command-line-tools:tagger-cli:run --args="..."`
  - Test execution via `./gradlew installDist` then running scripts
  - Test execution via native binaries (if applicable)
  - Document which method is preferred for one-off testing vs production use
  - Verify each pattern works from project root
- [x] Create CLI_EXECUTION.md context document
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Location: `agents.d/context/CLI_EXECUTION.md`
  - Structure: token-optimized (no human prose, bullet format)
  - Include: build commands, execution patterns, working directory requirements, common pitfalls
  - Include: how to pass arguments for typical use cases (help, version, actual operations)
- [x] Update agents.d/context/index.md to reference new document
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add appropriate reference/trigger for when CLI execution guidance is needed
  - Ensure document is loaded when work involves CLI testing or execution
- [x] Validate document by executing both CLIs using documented patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Follow CLI_EXECUTION.md exactly to run tagger-cli --help
  - Follow CLI_EXECUTION.md exactly to run digger-cli --help
  - Verify instructions are complete and unambiguous
  - Test from both project root and module directories if relevant
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

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
  - Follow documented patterns to execute tagger-cli with arguments
  - Follow documented patterns to execute digger-cli with arguments
  - Rewrite document in token-optimized agent-facing format
- Results:
  - ✅ `./gradlew check` passed (BUILD SUCCESSFUL in 22s)
  - ✅ Tagger via npx: `calculate-version --format=json` returned valid JSON
  - ✅ Tagger via npx: `guide` displayed full guide text
  - ✅ Digger via npx: `current-contribution-data /path/to/repo` created output file
  - ✅ Digger via npx: works from different working directory with absolute paths
  - ✅ Both CLIs accept arbitrary arguments via npx method
  - ✅ Document rewritten by subagent using token-optimized format (109 lines, ~1400t → ~800t estimated)
  - ✅ Review confirms all critical patterns preserved
  
## Discovery Summary
**Final execution method for arbitrary arguments:**
- Build: `jsProductionExecutableCompileSync` (creates npm package)
- Execute: `npx --package=<path-to-package> <cli> <args>`
- Package location: `command-line-tools/build/js/packages/command-line-tools-{cli}/`

**Alternative documented: npm link for interactive use**
- `./gradlew :{cli}:jsLink` installs globally via symlink
- Then use `tagger <args>` or `digger <args>` directly
- Downsides: global pollution, stale symlinks after clean, requires permissions

**Investigation progression:**
1. Started with `jsNodeProductionRun` (works but no argument support)
2. Tried direct node execution of compiled JS (path/resource issues)
3. Misidentified several things as "broken" without asking (JVM execution, digger JS execution)
4. User guidance: investigate `jsLink` and npm link mechanics  
5. Discovered npm package structure and npx as clean alternative
6. Verified `jsProductionExecutableCompileSync` is minimal build task needed
7. Tested both CLIs with real arguments (all working)
8. Subagent rewrote document in token-optimized agent-facing format

**Key findings:**
- Kotlin/JS plugin creates npm package with bin scripts at `build/js/packages/`
- `jsProductionExecutableCompileSync` populates package (lighter than `jsCliTar`)
- npx allows local package execution without global install
- Bin scripts live in `src/jsMain/resources/bin/` (processed by KMP plugin)
- Package.json bin config points to `kotlin/bin/{cli}` within package
- npm link uses symlinks to global node_modules (avoid for non-interactive use)

**Lessons learned:**
- Should have asked before concluding things were "broken" vs "not yet implemented"
- Should have investigated `assemble` task dependencies more thoroughly upfront
- User guidance to check jsLink revealed the npm package structure pattern
- Token-optimized format significantly more efficient for agent context docs
