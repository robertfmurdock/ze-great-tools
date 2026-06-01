# CLI Markdown Externalization Investigation

## Goal
Identify and validate a practical path to move embedded CLI markdown content into standalone markdown files wherever feasible, improving maintainability and linkable GitHub documentation coverage.

## Constraints
- Follow the repository work-card process defined in `agents.d/context/WORK_CHECKLIST.md`.
- Keep behavior stable while investigating; this card should prioritize discovery, risk assessment, and implementation planning before broad refactors.
- Preserve current CLI help quality and existing test confidence during any pilot extraction.
- Semver intent (initial): `[none]` for investigation and planning updates only; reassess with user confirmation if implementation work expands scope.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Inventory markdown currently embedded in CLI codepaths and classify extraction candidates
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Capture which content is user-facing help/guide material vs runtime-only generated output
- [x] Define extraction architecture for maximum practical coverage
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Specify file layout, loading strategy, fallback behavior, and packaging implications for JVM/native distributions
- [x] Execute a pilot extraction on one representative CLI surface and evaluate results
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Validate rendering parity, test ergonomics, and linkability from GitHub docs
- [x] Produce rollout recommendations with sequencing and risk controls for applying this broadly
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Include clear stop/go criteria and module-by-module order
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): [none] — this card is for investigation/planning and should avoid changing shipped behavior unless explicitly expanded]

**Subagent authorization**: User authorized subagent delegation on 2026-05-31.

**Final refactor report** (2026-05-31):
- Reviewed commit 2e77755 (7 files)
- Quality: 1 minor issue (acceptable) - LoadHelpResource.js.kt function length 16 lines (justified by platform-specific path resolution)
- Cross-module validation: PASS
- No remediation required

**Inventory findings (2026-05-31)**:
- Located 8 user-facing help/guide pieces (~170+ lines) across Tagger CLI and Digger CLI
- Strong extraction candidates: Guide.kt content, command help with markdown tables/lists
- Architectural pattern: Centralized in `override fun help()` methods using CliktCommand's markdown rendering
- Runtime-only content (3 pieces) should stay embedded: dynamic greetings, contextual config help

**Rollout recommendations (2026-05-31)**:

### Extraction Candidates (Priority Order)
1. **Tagger CLI** (3 more files after Guide.kt pilot):
   - `Tagger.kt` (~40 lines) - tool overview, CI usage example
   - `CalculateVersion.kt` (~12 lines) - snapshot reasons table
   - `Tag.kt` (~14 lines) - workflow description
   
2. **Digger CLI** (4 files):
   - `Guide.kt` (~47 lines) - fit assessment, prerequisites, workflow
   - `Digger.kt` (~18 lines) - tool overview, command table
   - `CurrentContributionData.kt` (~24 lines) - output documentation
   - `AllContributionData.kt` (~12 lines) - contribution boundaries concept

### Stop/Go Criteria
**Stop if**: Resource loading fails in production npm packages (verify with `npm link` test before broader rollout)
**Go if**: Pilot markdown is linkable from GitHub README and CLI help renders identically

### Sequencing
1. Complete remaining Tagger CLI extractions (3 files) - verify npm package includes all help resources
2. Apply same pattern to Digger CLI (4 files)
3. Update READMEs to link to extracted markdown files for improved discoverability

### Risk Controls
- Keep commits atomic (one file extraction per commit) for easy rollback
- Verify `./gradlew check` passes after each extraction
- Test both `./gradlew :tagger-cli:jsCliTar` and actual npm package contents
- Ensure copyHelpResources task runs before jsCliTar to bundle resources

### Follow-up Considerations
- Native target support would require different resource strategy (code generation or embedding)
- Could extract markdown to shared `docs/cli/` directory instead of per-module resources if cross-linking becomes valuable
- Consider whether dynamic content (version numbers, generated tables) should stay embedded vs templated

**Pilot extraction results (2026-05-31)**:
- Successfully extracted Guide.kt help text (~35 lines) to `tagger-guide.md`
- Created multiplatform resource loading: `loadHelpResource()` with JVM + JS implementations
- JVM: Standard classloader resource loading
- JS: Node.js fs.readFileSync with path resolution for build artifacts
- Added `copyHelpResources` task to include resources in npm package distribution
- All tests pass (JVM + JS), rendering parity verified
- GitHub linkability achieved: markdown now visible in repo browser
- Commit: 2e77755

**Extraction architecture design (2026-05-31)**:
## File Layout
- `src/commonMain/resources/help/{command-name}.md` for each command's help text
- `src/commonMain/resources/help/{tool}-guide.md` for tool-level guides
- Example paths:
  - `tagger-cli/src/commonMain/resources/help/tagger.md`
  - `tagger-cli/src/commonMain/resources/help/calculate-version.md`
  - `tagger-cli/src/commonMain/resources/help/tagger-guide.md`

## Loading Strategy
- Use Kotlin Multiplatform resource loading (works for JVM + JS targets)
- Create helper function: `fun loadHelpResource(path: String): String`
- Load in `help()` method: `override fun help(context: Context) = loadHelpResource("help/tagger.md")`
- Preserves existing Clikt markdown rendering pipeline (no changes to `MordantMarkdownHelpFormatter`)

## Fallback Behavior
- If resource load fails: return embedded fallback string (degraded but functional)
- Log warning to stderr about missing resource
- Prevents CLI breakage if packaging misconfigured

## Packaging Implications
### JVM Distribution
- Resources automatically bundled in JAR via Gradle's `processResources` task
- No build changes needed (standard Kotlin/JVM resource mechanism)

### JS/Node Distribution  
- Resources bundled via `jsProcessResources` task (already in build.gradle.kts)
- Copied to npm package structure during `compileProductionExecutableKotlinJs`
- Accessible via Kotlin/JS resource API at runtime

### Native Distribution (if added later)
- Would need resource embedding strategy (Kotlin/Native has limited resource support)
- Consider code-generation alternative for native targets (convert .md → Kotlin string constants at build time)

## Test Impact
- Existing CLI tests continue to work (help text still rendered, just loaded differently)
- Can add resource-loading tests to verify markdown files are packaged correctly
- GitHub linkability: markdown files become first-class artifacts visible in repo browser

## Validation
- Commands: 
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests "*GuideTest*"`
  - `./gradlew :command-line-tools:tagger-cli:jsTest`
  - `./gradlew :command-line-tools:tagger-cli:check`
- Results: All tests pass, resource loading works for both JVM and JS targets
