Digger Fit Assessment and Workflow Guide

Use Digger when:

- You need contribution metadata generated directly from Git history.
- You want build scripts to consume consistent semver and story-id signals.
- You are comfortable using commit-message conventions as team policy.
- You have a downstream plan for consuming the extracted data (visualization, stats, reporting).

Do not use Digger when:

- Your repository does not use Git tags to mark version boundaries.
- You have no downstream plan for consuming the extracted contribution data.
- You need real-time work tracking rather than post-contribution analysis.

Prerequisites:

- Git tags must mark version boundaries consistently.
- Full git history required for all-contribution-data; at least back to last tag for current-contribution-data.
- Automated build environments must not use shallow clones that truncate tag history.

Optional (for metadata extraction):

- Commit message conventions enable extraction of story ID, semver, and ease metadata.

First-run workflow:

1. Ensure your repository uses tags to mark releases.
2. Run `digger current-contribution-data <repo-path>` to extract data for the current contribution window.
3. Inspect the generated JSON file to verify extracted metadata matches expectations.
4. Integrate into build scripts using --format=json for advanced automation with explicit status envelopes.
5. Pipe output to downstream tools (e.g., Coupling for visualization and statistics).

Best practices:

- Keep commit metadata consistent so output stays reliable.
- Use --format=json for automation, and text mode when writing artifact files.
- Validate regex overrides in automated builds before promoting them to shared scripts.

Regex override contract:
When customizing extraction patterns, regex overrides MUST include required named groups:

- storyIdRegex must contain (?<storyId>...) named capture group
- easeRegex must contain (?<ease>...) named capture group
  Failure to include these groups will cause runtime errors.

Workflow philosophy:

- Git history is the source of truth for extracted contribution signals.
- Commit content provides semver, story, and ease classification.
- Tag boundaries define the current contribution window.
- Output is designed for downstream tooling, not manual transcription.
- Digger is non-judgmental: extraction only, no validation or enforcement.

For command details and integration examples:
<https://github.com/robertfmurdock/ze-great-tools/tree/main/command-line-tools/digger-cli>
