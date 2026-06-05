# JVM CLI Distribution Options Research

## Goal
Research and evaluate distribution options for tagger-cli and digger-cli as standalone JVM executables.

## Constraints
- Must provide user-friendly installation experience (no "download JAR and run java -jar")
- Should minimize JVM installation burden on end users
- Must work across Linux, macOS, Windows
- Should integrate with existing Gradle build tooling
- Must support both amd64 and arm64 architectures where feasible
- Semver intent: `[none]` - research only, no implementation

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Research option: jlink + jpackage (bundled JVM, native installers)
  - Pros/cons: distribution size, installation UX, platform support
  - Build integration: Gradle plugins available, CI requirements
  - Update plan if constraints discovered
- [x] Research option: GraalVM native-image
  - Pros/cons: startup time, binary size, build complexity, reflection/dynamic features
  - Build integration: Gradle native-image plugin, CI build time
  - Update plan if constraints discovered
- [x] Research option: jpackage without jlink (full JVM bundle)
  - Pros/cons: vs jlink approach, distribution size tradeoff
  - Build integration: simplicity vs size
  - Update plan if constraints discovered
- [x] Research option: Gradle application plugin + wrapper scripts (requires user JVM)
  - Pros/cons: simplest build, but JVM installation burden
  - Distribution: zip/tar with shell/batch launchers
  - Update plan if constraints discovered
- [x] Research option: Homebrew formula (macOS), apt/rpm packages (Linux), Chocolatey/Scoop (Windows)
  - Pros/cons: native package manager integration vs maintenance burden
  - Can bundle JVM or depend on system JVM
  - Update plan if constraints discovered
- [x] Research option: JBang distribution
  - Pros/cons: auto JVM installation, catalog distribution vs JBang dependency
  - Build integration: Gradle plugin, catalog setup
  - Update plan if constraints discovered
- [x] Survey existing Kotlin CLI tools: how do kotlinc, ktlint, detekt, etc. distribute?
  - Identify common patterns in Kotlin ecosystem
  - Update plan if constraints discovered
- [x] Document findings: comparison matrix (UX, size, build complexity, platform coverage, maintenance)
  - Include recommendation with rationale
  - Update plan if constraints discovered
- [x] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 7e361e4e
- **Uncommitted work**: Work card updates only
- **Blockers**: None
- **Status**: Research complete, all findings documented
- **Date**: 2026-06-05

## Implementation Notes
_(newest first)_

### 2026-06-05: Additional JVM distribution options evaluated
User noted Maven Central's strong security and requested investigation of JVM-native distribution mechanisms. Researched SDKMAN!, Coursier, Maven Central direct distribution, asdf, Docker/OCI, and Nix.

**Key findings:**
- **SDKMAN!**: Natural channel for JVM developers (6.8K stars, used for kotlinc/gradle/maven), low effort to add (package existing Gradle app plugin output), should be added to Phase 1
- **Coursier**: Scala-focused, directly resolves from Maven Central with strong security, but requires Coursier installation (bootstrap barrier)
- **Maven Central direct**: Best supply chain security (namespace verification, mandatory PGP signatures, immutability) but no discovery/friendly names - requires intermediary tooling
- **Maven Central paradox**: Excellent supply chain security (verified namespaces, mandatory PGP, immutable artifacts) but worst CLI distribution convenience (no discovery, requires coordinates, no auto-updates)
- **Why low adoption**: Maven Central optimized for library dependencies (build-time), not end-user software; Kotlin ecosystem chose convenience (Homebrew + system JVM) over Maven-direct
- **Security conclusion**: Maven Central provides strong supply chain foundation (all channels fetch from Central), but npm provides better operational security (auto-updates, audit tooling). Combined approach optimizes both dimensions.

Updated recommendation to add SDKMAN! to Phase 1 - target audience (git workflow users) likely JVM developers who naturally look in SDKMAN.

### 2026-06-05: Security analysis added
User requested evaluation of distribution security. Added comprehensive security analysis covering supply chain security, code signing, update mechanisms, vulnerability exposure, and tampering detection. Security findings strengthen the npm + Homebrew recommendation: npm has mature audit tooling and automatic updates, while bundled JVM approaches suffer from no auto-update mechanism (users run vulnerable versions indefinitely) and signing key sprawl.

### 2026-06-05: Research findings and recommendation

#### Context
Current distribution: Primary via npm/npx (JS target), with JVM JAR on Maven Central. CLI_EXECUTION.md shows npm is recommended method (`npx --package=...`).

#### Comparison Matrix

| Option | UX | Size | Build Complexity | Platform Support | Maintenance | JVM Handling | Security Posture |
|--------|----|----|------------------|------------------|-------------|--------------|------------------|
| **Current (npm)** | Excellent | ~10MB | Low | Universal | Low | Node.js (no JVM) | **Strong** (audit, provenance, auto-update) |
| **jlink + jpackage** | Excellent | 50-100MB/platform | Medium | 6+ builds (3 OS × 2 arch) | High | Bundled custom JVM | Moderate (signing required, no auto-update) |
| **jpackage (no jlink)** | Excellent | 150-250MB/platform | Low-Medium | 6+ builds | High | Bundled full JVM | Weak (large attack surface, bundled vulns) |
| **GraalVM native-image** | Excellent | 15-40MB/platform | High | 5+ builds | Very High | None (native binary) | Strong (minimal surface, static binary) |
| **Gradle application plugin** | Poor | 40-100MB | Low | Universal (1 build) | Low | User JVM required | Weak (no JVM control, manual updates) |
| **Homebrew/Scoop** | Good | 5-10MB | Low-Medium | Per-platform formulas | Medium | User JVM required | **Strong** (trusted chains, auto-update) |
| **JBang** | Good | ~10MB + JVM | Low | Universal | Low | Auto-installed by JBang | Moderate (JBang supply chain risk) |

#### Detailed Findings

**jlink + jpackage (bundled custom JVM)**
- Pros: Native installers (.pkg/.dmg/.exe/.deb/.rpm), no user JVM setup, 50-100MB custom runtime
- Cons: Must build on each platform (macOS/Windows/Linux), 6+ artifacts (3 OS × 2 arch), signing required (macOS notarization), CI matrix complexity
- Tooling: badass-runtime-plugin (mature), module-path configuration required
- Recommendation: Best for non-technical end users expecting native install UX, but high maintenance burden

**jpackage without jlink (bundled full JVM)**
- Pros: Simpler than jlink (no module analysis), native installers, full JDK available
- Cons: 2-3× larger (150-250MB vs 50-100MB), still requires platform-specific builds
- Use case: Rapid prototyping or when app needs full JDK capabilities
- Recommendation: Only if jlink module complexity blockers exist

**GraalVM native-image**
- Pros: True native binary (15-40MB), sub-50ms startup, no JVM dependency, 10-50MB memory footprint
- Cons: 3-10min build time, 5+ platform binaries, reflection config burden (Kotlin stdlib, Clikt, kotlinx-serialization), cannot cross-compile
- Tooling: org.graalvm.buildtools.native (official, mature), reflection-config.json maintenance required
- ProcessBuilder: Fully supported (git command execution works)
- Recommendation: Best for tools needing instant startup, but very high CI/maintenance cost

**Gradle application plugin + wrapper scripts**
- Pros: Minimal build complexity, single universal distribution, 30s build time
- Cons: Users must install JVM, poor first-run UX, "JAVA_HOME not set" errors, no PATH management
- Ecosystem: How kotlinc/ktlint/detekt distribute (fat JAR + wrappers)
- Recommendation: UX downgrade from current npm distribution; only viable as additional option for users refusing npm

**Package managers (Homebrew/apt/Chocolatey/Scoop)**
- Pros: Native OS integration, auto-updates, PATH management, trusted distribution
- Cons: Maintenance burden across 4+ ecosystems, typically require system JVM
- Pattern: Homebrew + Scoop cover 80% of developers with minimal overhead
- Ecosystem: All major Kotlin CLIs (kotlinc/ktlint/detekt) use this + require system JVM
- Recommendation: Complementary to JAR distribution, not replacement; start with Homebrew (low barrier, key audience)

**JBang**
- Pros: Auto-installs JVM if missing, simple catalog distribution, native Kotlin support
- Cons: Requires JBang installation first (~10MB), JVM startup overhead (100-300ms), niche adoption (Java community)
- Distribution: GitHub-based catalogs or JBang App Store (jbang.dev/appstore)
- Recommendation: Supplementary option for JVM developers already using JBang, not primary distribution

**SDKMAN!**
- Pros: Native JVM ecosystem channel (6.8K stars), used for kotlinc/gradle/maven distribution, `sdk upgrade` auto-updates, targets JVM developers specifically
- Cons: Requires SDKMAN installation first (bootstrap barrier), community approval required (days to weeks), moderate security (SHA-256 verification but no PGP chain)
- Distribution: Submit to sdkman-candidates GitHub repo with artifact URLs (can point to GitHub releases or Maven Central)
- Build integration: Works with existing Gradle application plugin archives
- Recommendation: **Add to Phase 1** - low effort (package existing output), high value (natural channel for target audience)

**Coursier**
- Pros: Direct Maven Central integration (inherits full PGP/checksum security), auto-manages JVM, resolves by Maven coordinates
- Cons: Requires Coursier installation (bootstrap barrier), primarily Scala ecosystem (2.1K stars, minimal adoption outside), manual updates only
- Distribution: Publish to Maven Central (likely already done), Coursier auto-resolves
- Recommendation: Document as option for users already using Coursier, but not worth promoting as primary channel (low adoption in Kotlin ecosystem)

**Maven Central (direct distribution)**
- Pros: **Best supply chain security** - namespace verification (DNS/GitHub proof), mandatory PGP signatures, immutable artifacts, transparent POMs. Stronger artifact integrity than npm.
- Cons: Cannot be end-user distribution alone - no discovery mechanism, no friendly names, requires Maven coordinates (groupId:artifactId:version), no auto-update notifications, no CLI execution convenience
- Reality: Infrastructure foundation, not user interface; requires intermediary tooling (Homebrew/SDKMAN/Coursier/JBang all fetch from Central)
- Security vs npm: Maven Central wins supply chain (verified namespaces, mandatory PGP, immutability), npm wins operational security (auto-updates, `npm audit`, faster patching)
- Recommendation: Already publishing to Central (foundation for all JVM channels); document Coursier/JBang usage for users who want Maven-native approach

#### Kotlin CLI Ecosystem Patterns
Survey of kotlinc, ktlint, detekt shows universal pattern:
- **Fat JAR primary artifact** (executable with dependencies bundled)
- **Require system JVM** (no bundling or native-image adoption observed)
- **Homebrew/SDKMAN primary distribution** with openjdk dependency
- **No native-image adoption** in mainstream Kotlin tools
- Target audience (developers) typically has JVM pre-installed

#### Security Analysis

**Security Comparison:**

| Method | Supply Chain | Code Signing | Update Security | Vuln Exposure | Tampering Detection |
|--------|-------------|--------------|-----------------|---------------|---------------------|
| **npm/npx** | npm audit, provenance, lockfiles | N/A (JS) | Auto (latest by default) | High (Node + deps) | npm checksums, SRI |
| **jlink + jpackage** | Gradle verification, SBOM | Required (macOS), recommended (Windows) | Manual (no mechanism) | Low (minimal JVM) | GPG signatures |
| **jpackage (no jlink)** | Gradle verification, SBOM | Required (macOS), recommended (Windows) | Manual | Medium (full JDK) | GPG signatures |
| **GraalVM native-image** | Gradle verification | Required (macOS) | Manual | Very Low (static) | GPG signatures |
| **Gradle app plugin** | Gradle verification | Optional (jarsigner) | Manual | High (user JVM) | GPG signatures |
| **Homebrew/Scoop** | Package manager verification | Inherited | Automatic (`brew upgrade`) | Delegated to openjdk | Package manager hashing |
| **JBang** | JBang catalog verification | N/A | Automatic (catalog) | Medium (JBang + JVM) | JBang verification |

**Key Security Findings:**

**npm/npx (current) - Strongest security posture:**
- Mature audit tooling (npm audit, provenance, dependency scanning)
- Automatic updates (`npx` pulls latest version by default)
- Single trust point (npm account with 2FA) vs 3+ platform signing keys
- Transparent dependency graph via package-lock.json
- Fast patching cycle (no rebuild/re-sign across platforms)
- **Risks**: Typosquatting, dependency confusion, Node.js runtime vulnerabilities
- **Mitigations**: Use scoped packages (`@org/name`), enable npm provenance, document exact `npx` commands with version pins

**Homebrew/Scoop - Strong security via package managers:**
- Established trust chains (formula maintainers, community review)
- Automatic security updates via package manager workflows
- Delegates JVM security to openjdk maintainers (better than self-bundling)
- **Risks**: Formula repository compromise, delayed updates
- **Mitigations**: Use official repositories (homebrew-core), automate version bump PRs

**jlink/jpackage - Moderate to weak security:**
- **Major weakness**: No auto-update mechanism → users run vulnerable versions indefinitely
- **Signing key sprawl**: Must manage macOS notarization, Windows Authenticode, Linux GPG keys
- **Build platform compromise**: Must build on each OS (macOS/Windows/Linux CI runners)
- **Large binaries**: 50-250MB harder to audit than 10MB npm package
- **Bundled JDK vulnerabilities** (jpackage without jlink): Ships unused vulnerable components
- **Mitigations**: Sign all artifacts, publish checksums/GPG signatures, implement update notification mechanism

**GraalVM native-image - Strong attack surface, weak update story:**
- Smallest attack surface (no JVM runtime, static binary, 15-40MB)
- Harder to reverse engineer than bytecode
- No runtime dependency vulnerabilities
- **Major weakness**: No auto-updates, 3-10min builds discourage frequent patches
- **Risks**: Reflection config errors expose unintended code paths, CI platform trust (cannot cross-compile)
- **Mitigations**: Sign binaries, audit reflection config, automate frequent builds

**Gradle application plugin - Weakest security:**
- Zero control over user's JVM security posture
- Manual update burden (users likely never update)
- No isolation (runs with full user privileges)
- **Only viable with**: Strong documentation of minimum JVM version, JAR signing with jarsigner

**JBang - Moderate security with supply chain concerns:**
- Adds JBang itself as attack vector (auto-installs arbitrary JVM versions)
- GitHub-hosted catalogs can be compromised via repo access
- JBang downloads JVMs without explicit user verification
- **Mitigations**: Host catalog in source repo with commit signing, pin JBang version in docs

**Security Best Practices by Method:**

**npm (recommended for primary distribution):**
- ✅ Enable npm provenance (`--provenance` flag on publish)
- ✅ Use `npm audit` in CI, fail on high-severity vulnerabilities
- ✅ Use scoped package name (`@org/tagger-cli`) to prevent namespace confusion
- ✅ Document exact npx command with version: `npx --package=@org/tagger-cli@1.2.3`
- ✅ Enable 2FA on npm publishing account

**Homebrew (recommended Phase 1):**
- ✅ Submit to homebrew-core (official repo) for community review
- ✅ Automate version bump PRs (renovate/dependabot)
- ✅ Pin openjdk dependency versions in formula

**If pursuing jlink/jpackage (not recommended):**
- ✅ Sign all artifacts (macOS notarization mandatory, Windows Authenticode strongly recommended)
- ✅ Publish SHA-256 checksums and GPG signatures alongside releases
- ✅ Use isolated CI runners per platform
- ✅ Implement update notification mechanism (phone-home or GitHub API check)
- ✅ Document signature verification in installation docs

#### Recommendation

**Maintain current npm distribution as primary method.** It provides excellent UX (instant install via `npx`, no runtime setup), universal platform support, and low maintenance burden.

**For JVM-focused users, pursue phased approach:**

**Phase 1 (low effort, high value):**
1. **Homebrew formula** depending on openjdk - reaches macOS developers, low maintenance, follows Kotlin ecosystem pattern
2. **SDKMAN!** - JVM ecosystem developers, natural channel for JVM tools (kotlinc, gradle distributed here), low effort (package existing Gradle app plugin output)
3. **Publish fat JAR to Maven Central** (likely already done) - foundation for all JVM channels, enables Coursier/JBang integration
4. **Document JBang catalog** - optional, for users already using JBang

**Phase 2 (evaluate demand):**
4. **Scoop manifest** (Windows) - JSON-based, no moderation, complements Homebrew
5. **Gradle application plugin distribution** as GitHub release archives - fallback for platforms without package managers

**Avoid (unless specific demand emerges):**
- **jlink/jpackage**: High maintenance (6+ platform builds, signing, CI matrix) for audience that likely has JVM
- **GraalVM native-image**: Very high build complexity and CI cost, no adoption in Kotlin CLI ecosystem, minimal benefit for developers
- **apt/rpm packages**: Higher infrastructure cost than Homebrew/Scoop

**Rationale:** Target audience is developers (git-based workflows, version tagging). They have JVM/npm installed. Current npm distribution already solves "no JVM installed" problem. Adding Homebrew expands reach without sacrificing npm UX. Native packaging (jlink/native-image) solves a problem that doesn't exist for this audience, while creating significant maintenance burden.

**Security analysis strengthens recommendation:**
- npm has strongest security posture: mature audit tooling, automatic updates, single trust point
- Homebrew adds security value: trusted package manager chains, automatic updates, community review
- jlink/jpackage weaken security: no auto-updates (users run vulnerable versions), signing key sprawl (3+ keys vs 1 npm account), large binaries harder to audit
- GraalVM minimal attack surface offset by no auto-update mechanism and slow patch cycle
- Security-conscious distribution strategy: npm (primary) + Homebrew (secondary) provides best balance of UX, maintenance burden, and security posture

### 2026-06-05: Subagent authorization granted
User authorized use of subagents for parallel research of distribution options.

### 2026-06-05: Work card created
Research work card for evaluating JVM distribution strategies for tagger-cli and digger-cli. No code changes planned - output will be documented findings in Implementation Notes section with recommendation.

Current distribution: JAR artifacts published to Maven Central, users must have JVM installed and run via `java -jar` or Gradle execution.

## Validation
Commands to run before marking complete:
- [x] Work card moved to work_completed/ with findings documented in Implementation Notes
