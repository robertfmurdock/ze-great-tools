# CLI JSON Output Schemas

This document describes the JSON schemas for all CLI tools in this repository when using `--format=json`.

## Common Response Structure

All commands follow a consistent envelope structure:

### Success Response

```json
{
  "status": "success",
  "data": {
    ...command-specific data...
  }
}
```

### Error Response

```json
{
  "status": "error",
  "error": "Human-readable error message",
  "code": "ERROR_CODE_CONSTANT"
}
```

## Tagger CLI

### calculate-version

Returns version information including snapshot status and reasons.

**Schema:**

```typescript
{
  status: "success",
  data: {
    version: string,           // Semantic version string (e.g., "1.2.3-SNAPSHOT")
    snapshot: boolean,         // true if this is a snapshot version
    snapshotReasons: string[]  // Array of reasons (e.g., ["DIRTY", "AHEAD"])
  }
}
```

**Example:**

```json
{
  "status": "success",
  "data": {
    "version": "1.2.3-SNAPSHOT",
    "snapshot": true,
    "snapshotReasons": ["DIRTY", "AHEAD"]
  }
}
```

**Snapshot Reasons:**

- `DIRTY`: Working directory has uncommitted changes
- `AHEAD`: Local branch is ahead of remote
- `NO_TAG`: No previous tags found
- `DETACHED_HEAD`: HEAD is detached (not on a branch)

**Error Codes:**

- `CONFIGURATION_ERROR`: Invalid configuration or repository state

### tag

Creates a git tag and optionally pushes it to the remote.

**Schema:**

```typescript
{
  status: "success",
  data: {
    tag: string  // The tag name that was created
  }
}
```

**Example:**

```json
{
  "status": "success",
  "data": {
    "tag": "1.2.3"
  }
}
```

**Error Codes:**

- `TAG_ERROR`: Failed to create or push tag

## Digger CLI

### current-contribution-data

Returns contribution data for the current development period (commits since the last tag).

**Schema:**

```typescript
{
  status: "success",
  data: {
    storyId: string | null,              // Story identifier from commit messages
    contributors: Array<{
      email: string,
      name: string
    }>,
    commits: Array<{
      sha: string,                       // Git commit SHA
      message: string,                   // Full commit message
      dateTime: string,                  // ISO 8601 timestamp
      author: string,                    // Author email
      ease: number | null                // Joy/ease score (1-5)
    }>,
    semver: "Major" | "Minor" | "Patch" | "None",  // Semantic version type
    label: string,                       // Repository/project label
    firstCommitDateTime: string,         // ISO 8601 timestamp
    lastCommitDateTime: string,          // ISO 8601 timestamp
    ease: number | null,                 // Aggregated ease score
    cycleTime: string | null             // ISO 8601 duration
  }
}
```

**Example:**

```json
{
  "status": "success",
  "data": {
    "storyId": "PROJ-123",
    "contributors": [
      {
        "email": "john@example.com",
        "name": "John Doe"
      }
    ],
    "commits": [
      {
        "sha": "abc123def456",
        "message": "[PROJ-123] [patch] Fix bug in validation",
        "dateTime": "2026-05-19T10:30:00Z",
        "author": "john@example.com",
        "ease": 4
      }
    ],
    "semver": "Patch",
    "label": "my-project",
    "firstCommitDateTime": "2026-05-19T10:30:00Z",
    "lastCommitDateTime": "2026-05-19T10:30:00Z",
    "ease": 4,
    "cycleTime": "PT2H30M"
  }
}
```

**Field Details:**

- `storyId`: Parsed from commit messages using square brackets (e.g., `[PROJ-123]`)
- `semver`: Determined by commit message tags (`[major]`, `[minor]`, `[patch]`, `[none]`)
- `ease`: Joy/ease score parsed from commit messages (e.g., `-4-` means ease=4)
- `cycleTime`: ISO 8601 duration between first and last commit
- Timestamps use ISO 8601 format with timezone (typically UTC)

### all-contribution-data

Returns all contribution data from the repository history, subdivided by tags.

**Schema:**

```typescript
{
  status: "success",
  data: Array<{
    // Same structure as current-contribution-data
    storyId: string | null,
    contributors: Array<{...}>,
    commits: Array<{...}>,
    semver: "Major" | "Minor" | "Patch" | "None",
    label: string,
    firstCommitDateTime: string,
    lastCommitDateTime: string,
    ease: number | null,
    cycleTime: string | null
  }>
}
```

**Example:**

```json
{
  "status": "success",
  "data": [
    {
      "storyId": "PROJ-123",
      "contributors": [...],
      "commits": [...],
      "semver": "Patch",
      "label": "my-project",
      "firstCommitDateTime": "2026-05-19T10:30:00Z",
      "lastCommitDateTime": "2026-05-19T11:00:00Z",
      "ease": 4,
      "cycleTime": "PT30M"
    },
    {
      "storyId": "PROJ-122",
      "contributors": [...],
      "commits": [...],
      "semver": "Minor",
      "label": "my-project",
      "firstCommitDateTime": "2026-05-18T14:00:00Z",
      "lastCommitDateTime": "2026-05-19T09:00:00Z",
      "ease": 3,
      "cycleTime": "PT19H"
    }
  ]
}
```

## Parsing Examples

### Using jq

```bash
# Extract version
tagger calculate-version --format=json | jq -r '.data.version'

# Check if snapshot
tagger calculate-version --format=json | jq -r '.data.snapshot'

# Get snapshot reasons
tagger calculate-version --format=json | jq -r '.data.snapshotReasons[]'

# Extract story ID
digger current-contribution-data $(pwd) --format=json | jq -r '.data.storyId'

# Get all contributor emails
digger current-contribution-data $(pwd) --format=json | jq -r '.data.contributors[].email'

# Get semver type
digger current-contribution-data $(pwd) --format=json | jq -r '.data.semver'

# Extract all story IDs from history
digger all-contribution-data $(pwd) --format=json | jq -r '.data[].storyId'
```

### Using Node.js

```javascript
const { execSync } = require('child_process');

// Get version
const output = execSync('tagger calculate-version --format=json', { encoding: 'utf8' });
const result = JSON.parse(output);
console.log(result.data.version);

// Get contribution data
const diggerOutput = execSync('digger current-contribution-data $(pwd) --format=json', { encoding: 'utf8' });
const contribution = JSON.parse(diggerOutput);
console.log(contribution.data.storyId);
```

### Using Python

```python
import json
import subprocess

# Get version
result = subprocess.run(
    ['tagger', 'calculate-version', '--format=json'],
    capture_output=True,
    text=True
)
data = json.loads(result.stdout)
print(data['data']['version'])

# Get contribution data
result = subprocess.run(
    ['digger', 'current-contribution-data', '.', '--format=json'],
    capture_output=True,
    text=True
)
contribution = json.loads(result.stdout)
print(contribution['data']['storyId'])
```

## Error Handling

All errors in JSON mode return valid JSON with error information:

```json
{
  "status": "error",
  "error": "Human-readable error message",
  "code": "ERROR_CODE_CONSTANT"
}
```

**Error handling example (bash):**

```bash
OUTPUT=$(tagger calculate-version --format=json 2>&1)
STATUS=$(echo "$OUTPUT" | jq -r '.status')

if [ "$STATUS" = "error" ]; then
  ERROR_MSG=$(echo "$OUTPUT" | jq -r '.error')
  ERROR_CODE=$(echo "$OUTPUT" | jq -r '.code')
  echo "Error ($ERROR_CODE): $ERROR_MSG" >&2
  exit 1
fi

VERSION=$(echo "$OUTPUT" | jq -r '.data.version')
echo "Version: $VERSION"
```

## Schema Versioning

The JSON schemas are considered stable once published. Breaking changes to the schema structure will be accompanied by:

1. A major version bump of the CLI tool
2. Documentation of migration steps
3. Deprecation warnings where possible

Non-breaking additions (new optional fields) may be added in minor versions.

## Canonical Schema Reference

For the most up-to-date schema definitions, refer to the source code:

- Tagger JSON models: `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/JsonOutput.kt`
- Digger JSON models: `libraries/digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt`
