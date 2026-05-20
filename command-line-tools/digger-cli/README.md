# Digger CLI

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)
![NPM Version](https://img.shields.io/npm/v/git-digger?label=npm%20git-digger)

A program for extracting 'contribution data' from git repositories into JSON files. 

## Installation

You can install the tool using any NPM-like system.

### Local Example

```bash
npm i -D git-digger # this will install it into a project as a dev dependency

npx digger current-contribution-data $(pwd) # You can use npx to run a project's programs easily
```

### Global Example

```bash
npm i -g git-digger # this will install it globally into npm

digger current-contribution-data $(pwd) # Now it should be available via NPM's path on your shell.
```

## Commands

### CurrentContributionData

The `currentContributionData` task will collect the most recent contribution to the repository.

The most recent contribution is calculated by looking for the most recent, non-HEAD tag, and then including every commit after that until the current HEAD.

#### Output

The contribution data JSON is created at `./currentContributionData.json`.

It will include all fields listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

### AllContributionData

The `allContributionData` task will collect all the contributions in the git repository.

This is calculated by subdividing the repository by its tags, and each section becomes a contribution.

#### Output

The contribution data JSON is created at `./allContributionData.json`, as a JSON array.

It will include all fields listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

## Fields of Interest

### Authors

This will include all authors listed on the commit, including committer, author, and co-authors.

### Story ID

This is parsed out of the commit message by looking for square bracketed text that does not match semver.

eg:
commit message: `[Cowdog-42] [patch] I did that thing`
produces: { storyId: "Cowdog-42" }

### Semver

This is parsed out of the commit message by looking for the strings "[major]", "[minor]", "[patch]", or "[none]".

eg:
commit message: `[Cowdog-42] [patch] I did that thing`
produces: { semver: "Patch" }

### Label

All contributions from one repository will share the same label. By default, this will be the Gradle project's name.

This can be overridden by argument:

```bash
digger currentContributionData --label SomethingMoreExciting ${pwd}
```

### Ease

This is parsed out of the commit message by looking for a number between one and five, wrapped in dashes.

This field is inspired by https://www.scrumexpert.com/knowledge/measuring-joy-for-software-developers/

eg:
commit message: `-3- I did that thing`
produces: { ease: 3 }

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

**Fields:**
- `status`: Always `"success"` for valid operations
- `data`: The contribution data object (see [ContributionDataJson.kt](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt) for full schema)
- `data.storyId`: Story identifier parsed from commit messages
- `data.contributors`: Array of contributor objects with email and name
- `data.commits`: Array of commit objects
- `data.semver`: Semantic version type (`"Major"`, `"Minor"`, `"Patch"`, `"None"`)
- `data.label`: Repository or project label
- `data.firstCommitDateTime`: ISO 8601 timestamp of first commit
- `data.lastCommitDateTime`: ISO 8601 timestamp of last commit
- `data.ease`: Joy/ease score (1-5) parsed from commit messages

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
      "contributors": [...],
      "commits": [...],
      "semver": "Patch",
      "label": "my-project",
      "firstCommitDateTime": "2026-05-19T10:30:00Z",
      "lastCommitDateTime": "2026-05-19T10:30:00Z",
      "ease": 3
    },
    {
      "storyId": "STORY-124",
      ...
    }
  ]
}
```

The `data` field contains an array of contribution data objects, one for each contribution period.

### CI Integration Examples

**Extract story ID in GitHub Actions:**
```yaml
- name: Get current contribution
  id: contribution
  run: |
    STORY_ID=$(digger current-contribution-data $(pwd) --format=json | jq -r '.data.storyId')
    echo "story-id=$STORY_ID" >> $GITHUB_OUTPUT

- name: Use story ID
  run: echo "Current story: ${{ steps.contribution.outputs.story-id }}"
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
