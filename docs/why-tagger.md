# Why Tagger? (Q/A Draft)

This FAQ captures current Tagger philosophy in probing-question form.

## 1) Problem Framing

### Q: What concrete release pain are we solving that plain Git tags + manual discipline do not solve?
**A:** Manual semver decisions are vulnerable to emotional pressure and inconsistency. Tagger exists to make versioning deterministic and neutral by deriving release signals from repository facts.

### Q: Which incidents or failure patterns justify introducing this tool?
**A:** Common patterns are manual versioning drift, weak tag discipline, unclear build/version traceability, and recurring arguments about bump frequency/level. Highest-cost observed failures include duplicate/no version labels and missed manual version updates that block downstream consumers.

### Q: What is the cost of being wrong: bad version numbers, bad tags, broken automation, or all three?
**A:** All three matter, but impact ranking is: (1) broken automation, (2) bad tags, (3) bad version numbers. Broken automation erodes trust in the whole delivery system fastest.

### Q: If we removed Tagger tomorrow, what would actually break versus just become less convenient?
**A:** Version calculation/tagging would fall back to manual or replacement automation. Release correctness pressure would weaken, and commit semver signaling discipline would likely degrade.

## 2) Version Source of Truth

### Q: Why should Git tags be the primary version source instead of build metadata, artifact repository state, or commit SHA?
**A:** Tags keep content changes and version metadata separate, avoid commit/build/version loop thrash, and provide a stable VCS pointer. The intended chain is `code -> tag -> build -> artifact`.

### Q: What guarantees do annotated tags provide that lightweight tags do not, and why do we care?
**A:** Tagger currently depends on tag metadata and deterministic ordering (for example `taggerdate`). Lightweight tags do not provide the same metadata contract.

### Q: How do we prevent version drift when multiple branches or CI jobs race?
**A:** Keep release evolution linear: one release truth, snapshot off non-release branches, and serialized release jobs on the release branch.

### Q: How do we handle historic tag mistakes without rewriting reality in dangerous ways?
**A:** Distinguish artifact reality from repo metadata. Public artifact mistakes are forward-fix only. Unpublished tag-only mistakes can be corrected in git metadata.

## 3) Semver Signal Strategy

### Q: Why are commit message tokens (`[major]`, `[minor]`, `[patch]`, `[none]`) the right mechanism?
**A:** They are low-friction and force a semver-impact decision at commit time, preserving fast commit flow while keeping compatibility intent explicit.

### Q: Why not Conventional Commits, PR labels, or explicit release manifests?
**A:** They are not rejected in principle. Current default prefers minimal cognitive overhead. Future support is acceptable where it fits core assumptions.

### Q: How do we prove this approach is reliable across teams with inconsistent commit hygiene?
**A:** Reliability comes from fast feedback, not perfection. Unexpected bumps surface quickly; teams adjust behavior through user feedback and review.

### Q: What is the failure mode when commits are mislabeled or unlabeled?
**A:** Unlabeled commits follow `implicitPatch` policy. Mislabeled commits produce wrong bump signals unless caught before publish, then corrected by follow-up release.

### Q: Why is `implicitPatch=true` a sane default rather than a hidden risk amplifier?
**A:** It is a deliberate bias-to-release. In many workflows, release stagnation is riskier than patch-overpublication, and semver consumers can stay on prior versions.

## 4) Snapshot and Safety Policy

### Q: Why should snapshot eligibility depend on repo state (`DIRTY`, `AHEAD`, `BEHIND`, `NOT_RELEASE_BRANCH`)?
**A:** `-SNAPSHOT` is a releasability signal. Those repo checks are version-correctness and release-safety checks expressed in one mechanism.

### Q: Which safety checks are mandatory versus policy choices?
**A:** Mandatory boundary: tag-based version tracking plus safe-branch posture for stable releases. Outside that boundary (for example strictness toggles) is team policy with explicit risk ownership.

### Q: Why is detached HEAD blocked by default?
**A:** Without known branch context, release policy application is ambiguous and stable-version decisions are riskier.

### Q: Under what circumstances is `allowDetachedHead=true` acceptable, and who owns that risk?
**A:** Only as an explicit escape hatch in controlled contexts with compensating safeguards. The enabling team owns the risk.

### Q: Why is "warnings as errors" optional instead of always on in CI?
**A:** Local development often benefits from non-blocking warnings, while CI can choose stricter enforcement. Tagger surfaces risk; team policy decides blocking behavior.

## 5) Release Branch Model

### Q: Why assume a designated release branch model at all?
**A:** Default is trunk-based with one release branch to prevent unexpected stable releases from arbitrary builds.

### Q: How does this design hold up for trunk-based development, release trains, or mono-repo multi-stream releases?
**A:** Strong fit for trunk-based and release-train models. Independent multi-version monorepo streams are not first-class today; single shared version-line monorepos are workable.

### Q: What happens when organizations rename branches, use multiple release branches, or backport hotfixes?
**A:** Branch rename is a config update. Multiple release branches are possible but add governance complexity. Hotfix branches from historical tags are valid when branch-local config is explicit.

### Q: Are we encoding workflow ideology as tooling constraints?
**A:** Yes, intentionally. Delivery tooling formalizes workflow ideology into executable policy.

## 6) Workflow Design (Two-Step vs One-Step)

### Q: Why split `calculateVersion` from `tag/release`?
**A:** To separate read-only version computation from side effects, improving local build stability and operational clarity.

### Q: What concrete engineering constraints justify this?
**A:** Gradle configuration-cache behavior, side-effect isolation, CI step handoff needs, and the practical need to validate builds before tagging/publishing.

### Q: What errors become less likely with two steps, and what new errors become more likely?
**A:** Less likely: premature/unvalidated release actions. More likely: teams overriding/ignoring computed versions and creating drift.

### Q: Why not one atomic command with dry-run and transactional behavior?
**A:** Valid future direction, but it risks pulling Tagger into broader build-system orchestration scope. Any such move should preserve toolchain compatibility and scope discipline.

## 7) CI/CD and Operational Reality

### Q: Why does Tagger depend on full history (`fetch-depth: 0`) and upstream tracking?
**A:** Safe version computation needs sufficient tag/graph context and sync-state visibility.

### Q: Is that requirement practical for large repos and cost-sensitive pipelines?
**A:** It has been practical at current scale; larger environments may need optimization, but not at the expense of version correctness.

### Q: How do we keep behavior stable across GitHub, GitLab, Azure, and local/dev environments?
**A:** Tagger is platform-agnostic; stability comes from enforcing identical prerequisites and invocation policy across environments.

### Q: Which assumptions about CI checkout behavior are fragile?
**A:** Real branch ref presence, full history/tags, upstream tracking, and tag push permissions are all common fragility points and should be surfaced early.

## 8) UX and Learnability

### Q: Can a junior developer predict outcomes without reading source code?
**A:** That is the target bar. If source reading is required to predict outcomes, that is a UX/docs defect.

### Q: Are error messages actionable enough to fix the issue on first attempt?
**A:** Currently good enough for common failures, with ongoing room for clarity improvements.

### Q: Where are we forcing users to memorize policy instead of surfacing it in command output?
**A:** Most runtime use is automated; main human memory burden is commit semver signaling.

### Q: Do docs explain why the guardrails exist, or just how to bypass them?
**A:** This is still an improvement area; rationale coverage should be strengthened.

## 9) Comparisons and Alternatives

### Q: Why this tool over `semantic-release`, `release-please`, `Changesets`, plain Gradle/Maven version automation, or homegrown scripts?
**A:** Comparative claim is scoped to direct experience: past `semantic-release` fit was unsatisfactory; Tagger has compared favorably to prior homegrown scripts used in practice.

### Q: What capability do we uniquely provide, and what ecosystem maturity are we giving up?
**A:** Core value is focused, consistent version-signal behavior. Full maturity tradeoff mapping against all alternatives is still unknown.

### Q: Are we solving a general problem or optimizing for one team's habits?
**A:** General problem. The same release/version signal failures recur across many projects.

## 10) Governance and Change Risk

### Q: Who can change versioning rules, and how is that change reviewed?
**A:** Whoever controls `.tagger`, build scripts, and CI invocation controls policy. Governance should protect those surfaces explicitly.

### Q: How do we prevent silent policy drift via regex/config changes?
**A:** Treat config/invocation changes like high-impact code and apply normal team governance controls (review gates, ownership, protections, CI checks).

### Q: What is our deprecation policy for flags/settings, and how painful are migrations?
**A:** Strong backward-compatibility intent, currently indefinite deprecation support, and intentionally small config/API surface to reduce migration pain.

### Q: How do we validate backward compatibility of behavior that downstream release pipelines rely on?
**A:** Functional coverage of core behavior across both CLI and Gradle plugin paths.

## 11) Failure Modes and Recovery

### Q: What are the top 5 ways this tool can cause a bad release?
**A:** The most clearly observed mode is incorrect semver signaling in commit messages. Other scenarios are currently more speculative than observed.

### Q: How do we detect each failure early?
**A:** Current primary loop is downstream/user feedback plus maintainer review and fast follow-up releases.

### Q: What is the rollback story after an incorrect tag or version is published?
**A:** Public artifact mistakes are forward-fixed with a newer release. Internal/unpublished tag mistakes can be corrected in repository metadata.

### Q: Can we recover safely without force-pushing tags or rewriting shared history?
**A:** Preferred posture is yes: avoid rewrites unless impact is strictly internal and downstream-safe.

## 12) Exit Strategy

### Q: If Tagger no longer fits, how do we migrate off it with minimal disruption?
**A:** Replace/remove the two explicit pipeline steps (`calculate`, then `tag/release`) with the successor flow.

### Q: Can existing tags/history be interpreted consistently by replacement tooling?
**A:** Unknown at present; this has not been broadly validated across alternative tools.

### Q: What lock-in are we creating in commit conventions, CI config, and team process?
**A:** Lock-in is considered lightweight. Convention changes should not require history rewrites.

## 13) Evidence We Should Demand

### Q: What objective metrics would prove Tagger is net-positive?
**A:** Increased release frequency.

### Q: Which baseline should we compare against?
**A:** Pre-Tagger history in the same repository.

### Q: What data would convince us to simplify or remove it?
**A:** Configuration churn: if settings need changes more than once every six months, simplify or reconsider.
