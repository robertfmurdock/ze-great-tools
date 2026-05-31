# Why Digger?

Digger is opinionated on purpose. This page is for deciding quickly whether those opinions fit your workflow.

## Digger Principles

1. **Git history is the mechanical source of truth for contributions.**
2. **Tags define contribution boundaries.**
3. **Commit messages encode contribution metadata (story ID, semver, ease).**
4. **Output is designed for downstream tooling, not direct human consumption.**
5. **Extraction is non-judgmental; decision-making happens in consuming tools.**

If those sound wrong for your team, Digger is likely a poor fit.

## Not For You If...

- Your repository does not use Git tags to mark version boundaries.
- You have no downstream plan for consuming the extracted contribution data.
- You need real-time work tracking rather than post-contribution analysis (Note: Digger supports real-time via `current-contribution-data`, but the primary use case is analytical retrospective).

## Scope Boundary

Digger is responsible for extracting contribution data from git (commits, tags, timing, contributors, metadata).

Digger is not responsible for visualization, statistical analysis, pairing correlation, actionable insights, validation, or enforcement. It surfaces data in a consistent shape; consuming tools (e.g., Coupling) handle interpretation and presentation.

## Fast "Should We Use It?" Questions

### Q: Who is Digger a good fit for?

**A:** Teams that:

- Use Git tags to mark version boundaries
- Have a downstream plan for consuming contribution data (visualization, statistics, reporting)
- Want privacy-controlled, universally git-compatible extraction (no external API dependencies)
- Need contribution analytics from git history (timing patterns, individual characteristics)

### Q: Who is Digger a poor fit for?

**A:** Teams without Git tags for releases, or teams with no downstream plan for the extracted data.

### Q: What does Digger reduce well?

**A:** Manual contribution data aggregation, inconsistent contribution boundary definitions, dependency on external service APIs for contribution analytics.

### Q: What does Digger not magically fix?

**A:** Insight generation, statistical analysis, data visualization. Digger extracts; consuming tools interpret.

## Important Tradeoffs

### Benefits you get

- Privacy-controlled extraction (data stays inside org walls)
- Universal git support (works anywhere git works, no GitHub/GitLab dependency)
- Consistent contribution data shape with maximum extractable information from git
- Focused on main-line releases (mechanical constraint simplifies boundary detection)

### Costs you accept

- Tags must mark version boundaries consistently
- Full git history required for `all-contribution-data`; at least back to last tag for `current-contribution-data`
- CI environments must avoid shallow clones that truncate tag history
- Requires downstream tooling for value realization (extraction alone provides no insights)

## Failure Modes

### Most observed failures

- **Story ID regex mismatches**: Project-specific commit conventions require regex tuning
- **Inconsistent tag discipline during transition**: Resolves once team fully adopts tag-based releases

### Recovery posture

- Commit to consistent tagging practices
- Tune regex patterns for project conventions
- Use regex override contract (required named groups: `storyId`, `ease`) when customizing extraction

## Success Criteria

Digger works well when:

- Downstream analysis/process works consistently
- Operational insights are delivered over time
- Team doesn't have to think about digger—it just works
- CI integration is stable and predictable

Success is indirect: Digger is infrastructure for insight-generating processes, not the insights themselves.

## Alternatives Context

Digger was built rather than using existing git analytics tools because:

1. **Privacy/control**: Derived data must stay inside org walls; no external API dependencies acceptable
2. **Universal git support**: Must work anywhere git works, not tied to GitHub/GitLab
3. **Simplicity**: Straightforward enough to build directly rather than research/evaluate alternatives

## Relationship to Other Tools

- **Tagger**: Defines version boundaries that Digger uses as contribution windows
- **Coupling**: Consumes Digger output for visualization, statistics, and pairing correlation
- **External tracking (JIRA, etc.)**: Complementary, not competitive—git is mechanical truth; tracking systems capture human time allocation
