# Why Tagger?

This is a short FAQ about what Tagger is for, what it is not for, and how we use it in practice.

## Scope Boundary

### Q: What is Tagger responsible for?

**A:** Computing versions from repo signals and applying tagging policy consistently.

### Q: What is Tagger not responsible for?

**A:** Product release strategy, deployment orchestration, feature rollout, incident response, or overall software
delivery performance. Tagger helps with version/tag correctness; it is one part of release engineering, not all of it.

## 1) Problem Framing

### Q: What concrete pain are we solving?

**A:** Manual versioning and tagging can get inconsistent under pressure. Tagger makes semver and tagging decisions
deterministic from repository facts.

### Q: If we removed Tagger tomorrow, what would change?

**A:** Version/tag decisions revert to manual discipline or replacement automation. Teams lose this policy enforcement
point, but the broader release system can still function.

## 2) Version Source of Truth

### Q: Why use Git tags as the version source?

**A:** Tags are durable VCS metadata and keep version identity separate from build mechanics. Intended flow:
`code -> tag -> build -> artifact`.

### Q: Why prefer annotated tags?

**A:** Tagger relies on tag metadata and ordering semantics (for example `taggerdate`) that lightweight tags do not
provide.

### Q: How do we handle tag mistakes?

**A:** Public artifact mistakes are forward-fixed. Internal, unpublished metadata mistakes may be corrected in Git
metadata.

## 3) Semver Signal Strategy

### Q: Why use commit tokens (`[major]`, `[minor]`, `[patch]`, `[none]`)?

**A:** They are low-friction and make semver intent explicit at change time.

### Q: Is this rejecting Conventional Commits or other models?

**A:** No. The current default optimizes for low overhead. Other signaling models are compatible future options.

### Q: What happens with missing or wrong labels?

**A:** Missing labels follow `implicitPatch`. Wrong labels produce wrong bumps unless caught before publish; recovery is
usually a follow-up release.

## 4) Safety Policy

### Q: Why gate behavior on repo state (`DIRTY`, `AHEAD`, `BEHIND`, `NOT_RELEASE_BRANCH`)?

**A:** These are version/tag safety signals, not full release-safety guarantees.

### Q: Why block detached HEAD by default?

**A:** Policy application is ambiguous without stable branch context.

### Q: Who owns risk when strictness is relaxed?

**A:** The team enabling exceptions (`allowDetachedHead`, warning strictness, etc.) owns that risk.

## 5) Branch Model

### Q: Why assume a designated release branch?

**A:** It constrains where stable tags can be created and reduces accidental stable releases from arbitrary builds.

### Q: Does this fit every repo topology?

**A:** Strong for single version-line repos (including many trunk-based setups). Multi-stream monorepo version lines are
not a first-class target.

## 6) Workflow Design

### Q: Why separate `calculateVersion` from `tag/release`?

**A:** It separates read-only version computation from side effects and keeps integration points clearer in CI.

### Q: What tradeoff does that create?

**A:** Lower risk of premature tagging, but higher risk of teams overriding computed results if process discipline is
weak.

## 7) CI Operational Prerequisites

### Q: Why require full history (`fetch-depth: 0`) and upstream context?

**A:** Version/tag decisions need enough graph and tag history to be correct.

### Q: What usually breaks in CI?

**A:** Shallow checkout, missing tags, missing branch refs, missing upstream tracking, or tag push permissions.

## 8) Governance and Change Risk

### Q: Where does policy actually live?

**A:** In `.tagger`, build scripts, and CI invocation. Control of those surfaces is control of release-tag policy.

### Q: How should policy changes be treated?

**A:** As high-impact changes: code review, ownership boundaries, and CI checks.

## 9) Failure Modes and Recovery

### Q: What failure is most clearly observed?

**A:** Incorrect semver signaling in commits.

### Q: What other failures are plausible?

**A:** CI history/tag incompleteness, branch policy misconfiguration, detached-HEAD usage, and inconsistent local vs CI
invocation.

### Q: What is the recovery posture?

**A:** Prefer forward fixes without rewriting shared history. Rewrite tags/history only for strictly internal,
downstream-safe corrections.

## 10) Evidence and Exit Criteria

### Q: How do we evaluate whether Tagger helps?

**A:** Use a small set: release frequency, lead time to release, change failure rate, rollback/repair time, plus
version/tag incident rate.

### Q: What baseline should we use?

**A:** Pre-Tagger behavior in the same repository and workflow.

### Q: When should we simplify or remove it?

**A:** If Tagger-specific policy churn stays high, incidents remain frequent, or outcomes are not better than a simpler
alternative over a meaningful period.

## Further Reading

### Core Release/Version References

- Semantic Versioning 2.0.0: https://semver.org/
- Git documentation, `git-tag`: https://git-scm.com/docs/git-tag
- Pro Git, *Tagging*: https://git-scm.com/book/en/v2/Git-Basics-Tagging
- Google SRE Book, *Release Engineering*: https://sre.google/sre-book/release-engineering/
- The Twelve-Factor App, *Build, release, run*: https://12factor.net/build-release-run

### CI and Branching

- Martin Fowler, *Continuous Integration*: https://martinfowler.com/articles/continuousIntegration.html
- Trunk Based Development, *Branch for release*: https://trunkbaseddevelopment.com/branch-for-release/
- DORA, *Trunk-based development capability*: https://dora.dev/capabilities/trunk-based-development/
- GitHub `actions/checkout` docs: https://github.com/actions/checkout

### Alternatives and Governance

- Conventional Commits 1.0.0: https://www.conventionalcommits.org/en/v1.0.0/
- semantic-release docs: https://semantic-release.gitbook.io/semantic-release/
- release-please docs: https://github.com/googleapis/release-please
- Changesets docs: https://github.com/changesets/changesets
- GitHub docs, protected
  branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- Open Policy Agent docs: https://www.openpolicyagent.org/docs/latest/

### Robert Murdock Writing

- *Continuous Integration Metrics*: https://www.zegreatrob.com/ContinuousIntegrationMetrics.html
- *Looking Back, Part 2: Continuous Integration
  Metrics*: https://www.zegreatrob.com/2023/05/19/ContinuousMetricsRevisited.html
- *Book Thoughts 2 (Escape Velocity)*: https://www.zegreatrob.com/2023/05/22/EscapeVelocityThoughts.html
- *The Quality of Test*: https://www.zegreatrob.com/2020/04/24/TheQualityOfTest.html
