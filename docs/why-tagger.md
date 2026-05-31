# Why Tagger?

Tagger is opinionated on purpose. This page is for deciding quickly whether those opinions fit your workflow.

## Tagger Principles

1. **Version Numbers live on annotated Git tags.**[^p1]
2. **Commit content determines the next version.**[^p2]
3. **Releases should be created on a release branch.**[^p3]
4. **Version calculation happens separately from release.**[^p4]
5. **Configuration lives in code and is governed the same way.**[^p5]

If those sound wrong for your team, Tagger is likely a poor fit.

## Not For You If...

- You want version truth to come from an artifact repository or build metadata instead of Git tags.
- You need multi-stream version lines as a first-class workflow.
- You want a single atomic command to orchestrate the entire release lifecycle.
- You are not willing to enforce Git/CI prerequisites (full history, tags, branch context).

## Scope Boundary

Tagger is responsible for version calculation and tagging policy enforcement.

Tagger is not a full release platform. It does not solve deployment orchestration, rollout strategy, incident response,
or product delivery management.

## Fast "Should We Use It?" Questions

### Q: Who is Tagger a good fit for?

**A:** Teams that want:

- deterministic versioning behavior
- willingness to use Git tags
- ability to enforce CI prerequisites consistently

### Q: Who is Tagger a poor fit for?

**A:** Teams that want artifact-repo-first version truth, multi-stream version lines as a first-class requirement, or
fully atomic "one command does everything" release orchestration.

### Q: What does Tagger reduce well?

**A:** Manual version drift, inconsistent bump decisions, and weak tag discipline.

### Q: What does Tagger not magically fix?

**A:** Broader release quality/process problems outside version/tag policy.

## Common Objections (And the Tagger Position)

### "Why Git tags as source-of-truth?"

Tagger treats tags as durable VCS release metadata and keeps version identity separate from build mechanics:
`code -> tag -> build -> artifact`.[^o1]

### "Why commit tokens instead of Conventional Commits / PR labels / manifests?"

The default favors low-friction signaling – we want programmers to habitually think in semver when working on libraries.
Alternative signaling schemes are valid; Tagger's core position is that bump intent should be explicit and
machine-readable. Other schemes may be explicitly supported in the future.[^o2]

### "Why split `calculate version` and `tag` into two steps?"

It isolates read-only computation from side effects. Tradeoff: it lowers the risk of premature tagging, but teams must
avoid overriding computed versions casually.[^o3]

### "Why enforce branch/snapshot/detached-HEAD rules?"

These checks are policy guardrails for version/tag correctness. They are not claims that Tagger guarantees release
safety end-to-end.[^o4]

### "Why require a full git history in CI?"

Version decisions depend on tag and graph context. Shallow or incomplete checkout is a common source of wrong
outcomes.[^o5]

## Important Tradeoffs

### Benefits you get

- Deterministic version/tag behavior from repository facts
- Lower semantic drift in versioning decisions
- Clear policy surface (`.tagger` + CI invocation)

### Costs you accept

- CI checkout and branch-context requirements
- Opinionated branch/release posture by default
- Need for governance around policy/config changes

## Failure Modes and Recovery Posture

### Most observed failure

- Incorrect semver signaling in commits

### Other plausible failures

- Missing tag/history context in CI
- Branch policy misconfiguration
- Detached-HEAD invocation in the wrong place
- Local/CI invocation mismatch

### Recovery posture

- Prefer forward-fix releases
- Avoid rewriting shared history/tags except strictly internal, downstream-safe cases

## Evidence and Exit Criteria

### How to evaluate fit and value

Track Tagger-scoped indicators:

- Version/tag incident rate (wrong bump, missing tag, duplicate/conflicting tag)
- Manual version/tag overrides per release window
- CI failures caused by missing tag/history/branch context
- Frequency of policy/config churn in `.tagger` and invocation settings
- Recovery rate for Tagger-caused mistakes using forward-fix only (no history rewrite)

Lead time, change failure rate, and rollback time are useful delivery metrics, but they primarily measure the whole
software pipeline rather than Tagger itself.

### Baseline

Compare against pre-Tagger behavior in the same repo and workflow.

### When to simplify or remove Tagger

If Tagger-specific churn stays high, incidents remain frequent, or outcomes are not better than a simpler alternative
over a meaningful period.

## Comparison with Similar Tools

Tagger shares philosophical alignment with tools like [semantic-release](https://semantic-release.gitbook.io/)
and [Axion Release Plugin](https://github.com/allegro/axion-release-plugin) around making releases mechanical and
unsentimental. However, they differ significantly in scope, integration approach, and version derivation philosophy.

For detailed comparisons that help you choose the right tool for your context, see:

**[Tagger vs Semantic-Release: Choosing the Right Tool](tagger-vs-semantic-release.md)**

Key differentiators:

- **Tagger**: Narrowly-scoped, scriptable tool focused on version calculation and tagging. Platform-neutral, stays out
  of your build process.
- **Semantic-release**: Full-lifecycle release orchestrator with integrated changelog generation, artifact publishing,
  and notifications.

**[Tagger vs Axion Release Plugin: Choosing the Right Tool](tagger-vs-axion-release.md)**

Key differentiators:

- **Tagger**: Commit messages contain increment instructions that determine how to bump from the last tag. Two-phase
  workflow enables validation before tagging.
- **Axion**: Automatic patch increment on each release, or manual major/minor specification. Single-command release
  workflow.

All approaches are valid—choose based on whether you prefer commit-driven increment instructions vs
automatic/manual versioning, focused composability vs integrated orchestration.

## Further Reading

<a id="support-core"></a>

### Core Release/Version References

- Semantic Versioning 2.0.0: https://semver.org/
- Git documentation, `git-tag`: https://git-scm.com/docs/git-tag
- Pro Git, *Tagging*: https://git-scm.com/book/en/v2/Git-Basics-Tagging
- Google SRE Book, *Release Engineering*: https://sre.google/sre-book/release-engineering/
- The Twelve-Factor App, *Build, release, run*: https://12factor.net/build-release-run

<a id="support-ci"></a>

### CI and Branching

- Martin Fowler, *Continuous Integration*: https://martinfowler.com/articles/continuousIntegration.html
- Trunk-Based Development, *Branch for release*: https://trunkbaseddevelopment.com/branch-for-release/
- DORA, *Trunk-based development capability*: https://dora.dev/capabilities/trunk-based-development/
- GitHub `actions/checkout` docs: https://github.com/actions/checkout

<a id="support-alt"></a>

### Alternatives and Governance

- Conventional Commits 1.0.0: https://www.conventionalcommits.org/en/v1.0.0/
- semantic-release docs: https://semantic-release.gitbook.io/semantic-release/
- release-please docs: https://github.com/googleapis/release-please
- Changesets docs: https://github.com/changesets/changesets
- GitHub docs, protected
  branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- Open Policy Agent docs: https://www.openpolicyagent.org/docs/latest/

<a id="support-author"></a>

### Writing by the Tagger Author

- *Continuous Integration Metrics*: https://www.zegreatrob.com/ContinuousIntegrationMetrics.html
- *Looking Back, Part 2: Continuous Integration
  Metrics*: https://www.zegreatrob.com/2023/05/19/ContinuousMetricsRevisited.html
- *Book Thoughts 2 (Escape Velocity)*: https://www.zegreatrob.com/2023/05/22/EscapeVelocityThoughts.html
- *The Quality of Test*: https://www.zegreatrob.com/2020/04/24/TheQualityOfTest.html

[^p1]: See [Core Release/Version References](#support-core).
[^p2]: See [Core Release/Version References](#support-core) and [Alternatives and Governance](#support-alt).
[^p3]: See [CI and Branching](#support-ci).
[^p4]: See [Core Release/Version References](#support-core).
[^p5]: See [Alternatives and Governance](#support-alt).
[^o1]: Related references: [Core Release/Version References](#support-core).
[^o2]: Related references: [Alternatives and Governance](#support-alt).
[^o3]: Related references: [Core Release/Version References](#support-core).
[^o4]: Related references: [CI and Branching](#support-ci).
[^o5]: Related references: [CI and Branching](#support-ci).
