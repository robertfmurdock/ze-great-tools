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
- The GitHub new-issue chooser cannot display these uncommitted local files. GitHub documents that forms appear after they are committed to the default branch; confirm the chooser after merge before closing this card.

## Resume Note
Implementation is ready for review. After these files reach the default branch, open the repository's new-issue chooser and confirm both forms appear, then move this card to `agents.d/work_completed/`.
