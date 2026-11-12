# Add Community Files to Repository

**Work card protocol: 2**

## Goal
Add standard community files (CONTRIBUTING.md, CODE_OF_CONDUCT.md, issue templates) to lower contribution barriers and signal a welcoming open source community.

## Constraints and acceptance evidence
- Semver intent: `[none]` — documentation only.
- Follow GitHub's recommended structure and align with project values: clarity, simplicity, and test-first discipline.
- Keep contributor guidance welcoming while preserving quality expectations.
- `CONTRIBUTING.md` covers environment setup, `./gradlew check`, test-first expectations, issue/PR submission, and links to `CLAUDE.md`.
- Add a standard Contributor Covenant code of conduct and structured bug report and feature request templates.
- Verify links, grammar, formatting, and rendered appearance using [DOCUMENTATION.md](../context/DOCUMENTATION.md). Confirm issue templates appear in GitHub's new issue flow.

## Decisions
- Prefer the standard Contributor Covenant to avoid maintaining a custom code of conduct.
- Keep issue templates structured but not overly prescriptive; they should prompt useful reports without making contribution harder.

## Implementation Notes
- Added `CONTRIBUTING.md` with JDK 21 setup, `./gradlew check`, test-first guidance, issue/PR guidance, and a link to `CLAUDE.md`.
- Added Contributor Covenant 2.1 in `CODE_OF_CONDUCT.md`; reports can be directed through the maintainer's GitHub contact links.
- Added bug report and feature request issue forms in `.github/ISSUE_TEMPLATE/`. Both parse as YAML and use GitHub's documented location and required form metadata.
- Verified local links, the Contributor Covenant and maintainer profile links, Markdown formatting, grammar (IDEA: no reported problems), YAML parsing, and `git diff --check`.
- GitHub's contents API confirms both YAML forms are present on `main`. The unauthenticated new-issue chooser redirects to sign-in, so its rendered choices could not be inspected from this session.
- Maintainer confirmed both forms appear in GitHub's authenticated new-issue chooser.

