# Digger CLI

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)
![NPM Version](https://img.shields.io/npm/v/@continuous-excellence/digger?label=npm%20@continuous-excellence/digger)

A program for extracting 'contribution data' from git repositories into JSON files.

## Installation

### NPM Installation (Recommended)

You can install the tool using any NPM-like system.

#### Local Example

```bash
npm i -D @continuous-excellence/digger # this will install it into a project as a dev dependency

npx digger current-contribution-data $(pwd) # You can use npx to run a project's programs easily
```

#### Global Example

```bash
npm i -g @continuous-excellence/digger # this will install it globally into npm

digger current-contribution-data $(pwd) # Now it should be available via NPM's path on your shell.
```

### JVM Distribution (Alternative)

For environments without Node.js, a standalone JVM distribution is available.

#### Build from Source

```bash
./gradlew :command-line-tools:digger-cli:jvmDistZip
unzip command-line-tools/digger-cli/build/distributions/digger-cli-jvm.zip -d /path/to/install
```

#### Distribution Structure

The JVM distribution archive contains:
- `bin/digger` - Unix/Linux/macOS executable script
- `bin/digger.bat` - Windows executable script
- `lib/` - All required JVM dependencies

Add the `bin` directory to your PATH for easy access:

```bash
export PATH="/path/to/digger-cli-jvm/bin:$PATH"
digger --version
```

### Migration from `git-digger`

The package has been renamed to `@continuous-excellence/digger`. The old `git-digger` package is deprecated. To migrate:

```bash
# Uninstall old package
npm uninstall git-digger

# Install new scoped package
npm install @continuous-excellence/digger
```

The CLI command remains `digger` - no changes needed to your scripts or workflows.

## Commands

### Current Contribution Data

The `current-contribution-data` command will collect the most recent contribution to the repository.

The most recent contribution is calculated by looking for the most recent, non-HEAD tag, and then including every commit
after that until the current HEAD.

#### Output

The contribution data JSON is created at `./currentContributionData.json`.

It will include all fields
listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

### All Contribution Data

The `all-contribution-data` command will collect all the contributions in the git repository.

This is calculated by subdividing the repository by its tags, and each section becomes a contribution.

#### Output

The contribution data JSON is created at `./allContributionData.json`, as a JSON array.

It will include all fields
listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

## Structured Output

Both commands support machine-readable JSON output for CI/CD pipelines and automation scripts via the `--format` flag.

### Format Options

- `--format=text` (default): Writes JSON to a file and prints a confirmation message
- `--format=json`: Outputs structured JSON to stdout wrapped in a status envelope

### Text Mode (Default)

**Example command:**

```bash
digger current-contribution-data $(pwd)
```

**Output:**

```
Data written to currentContributionData.json
```

The JSON data is written to `currentContributionData.json` (or the file specified by `--output-file`).

### JSON Mode

**Example command:**

```bash
digger current-contribution-data $(pwd) --format=json
```

**Success response:**

```json
{
  "status": "success",
  "data": {
    "storyId": "STORY-123",
    "contributors": [
      {
        "email": "user@example.com",
        "name": "John Doe"
      }
    ],
    "commits": [
      {
        "sha": "abc123",
        "message": "[STORY-123] [patch] Fix bug",
        "dateTime": "2026-05-19T10:30:00Z"
      }
    ],
    "semver": "Patch",
    "label": "my-project",
    "firstCommitDateTime": "2026-05-19T10:30:00Z",
    "lastCommitDateTime": "2026-05-19T10:30:00Z",
    "ease": 3
  }
}
```

The `data` field contains the contribution data object.
See [ContributionDataJson.kt](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt)
for the complete schema, or use `current-contribution-data --help` for field descriptions.

### AllContributionData JSON Mode

**Example command:**

```bash
digger all-contribution-data $(pwd) --format=json
```

**Success response:**

```json
{
  "status": "success",
  "data": [
    {
      "storyId": "STORY-123",
      "contributors": [],
      "commits": [],
      "semver": "Patch",
      "label": "my-project",
      "firstCommitDateTime": "2026-05-19T10:30:00Z",
      "lastCommitDateTime": "2026-05-19T10:30:00Z",
      "ease": 3
    }
  ]
}
```

(Additional contribution objects omitted for brevity)

The `data` field contains an array of contribution data objects, one for each contribution period. Use
`all-contribution-data --help` for more details.

### CI Integration Examples

**Extract story ID in GitHub Actions:**

```yaml
- name: Get current contribution
  id: contribution
  run: |
    STORY_ID=$(digger current-contribution-data $(pwd) --format=json | jq -r '.data.storyId')
    echo "story-id=$STORY_ID" >> $GITHUB_OUTPUT

- name: Use story ID
  run: |
    echo "Current story: ${{ steps.contribution.outputs.story-id }}"
```

**Extract contributor list in bash:**

```bash
# Get contributors
CONTRIBUTORS=$(digger current-contribution-data $(pwd) --format=json | jq -r '.data.contributors[].name')

echo "Contributors:"
echo "$CONTRIBUTORS"
```

**Check semver type:**

```bash
OUTPUT=$(digger current-contribution-data $(pwd) --format=json 2>/dev/null)
SEMVER=$(echo "$OUTPUT" | jq -r '.data.semver')

case "$SEMVER" in
  "Major")
    echo "Breaking change detected"
    ;;
  "Minor")
    echo "New feature detected"
    ;;
  "Patch")
    echo "Bug fix detected"
    ;;
  "None")
    echo "No version bump"
    ;;
esac
```

**Extract all story IDs:**

```bash
# Get all contributions and extract story IDs
STORY_IDS=$(digger all-contribution-data $(pwd) --format=json | jq -r '.data[].storyId' | sort -u)

echo "All story IDs:"
echo "$STORY_IDS"
```

## Help

For a full listing of the available options in the program, please use the built-in help command.

```bash
digger --help
```

For fit-assessment guidance and workflow recommendations:

```bash
digger guide
```

Documentation is also available as markdown files in the repository:

- [Digger Guide](src/commonMain/resources/help/digger-guide.md) - Fit assessment, philosophy, and workflow guidance
- [Digger Help](src/commonMain/resources/help/digger.md) - Main command overview and options
- [Current Contribution Data Help](src/commonMain/resources/help/current-contribution-data.md) - Command details and
  output format
- [All Contribution Data Help](src/commonMain/resources/help/all-contribution-data.md) - Command details and output
  format
