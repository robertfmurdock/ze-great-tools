# Tagger CLI

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)
![NPM Version](https://img.shields.io/npm/v/git-semver-tagger?label=npm%20git-semver-tagger)

An opinionated program for automatic semantic versioning via git tags and information in commits. 

## Installation

You can install the tool using any NPM-like system.

### Local Example

```bash
npm i -D git-semver-tagger # this will install it into a project as a dev dependency

npx tagger calculate-version # You can use npx to run a project's programs easily
```

### Global Example

```bash
npm i -g git-semver-tagger # this will install it globally into npm

tagger calculate-version # Now it should be available via NPM's path on your shell.
```

## Commands

### Calculate Version

The `calculate-version` command will generate a new version number based on all of the commits since the last tag, and output as a string.

**Basic usage:**
```bash
tagger calculate-version
```

**Output:** `1.2.3-SNAPSHOT`

### Tag

The `tag` command will create a tag with the given version and push it back to the repository.

We recommend this command only is run after the build is validated. Use discernment to decide if it should happen before publication of artifacts, or afterward.

**Basic usage:**
```bash
tagger tag --version 1.2.3 --release-branch main
```

## Structured Output

Both commands support machine-readable JSON output for CI/CD pipelines and automation scripts via the `--format` flag.

### Format Options

- `--format=text` (default): Human-readable text output
- `--format=json`: Structured JSON output

### Calculate Version JSON Output

**Example command:**
```bash
tagger calculate-version --format=json
```

**Success response:**
```json
{
  "status": "success",
  "data": {
    "version": "1.2.3-SNAPSHOT",
    "snapshot": true,
    "snapshotReasons": [
      "DIRTY",
      "AHEAD"
    ]
  }
}
```

**Error response:**
```json
{
  "status": "error",
  "error": "HEAD is detached (not pointing at any branch)",
  "code": "CONFIGURATION_ERROR"
}
```

**Fields:**
- `status`: `"success"` or `"error"`
- `data.version`: Calculated semantic version string
- `data.snapshot`: Boolean indicating if this is a snapshot version
- `data.snapshotReasons`: Array of reasons why this is a snapshot (e.g., `"DIRTY"`, `"AHEAD"`)
- `error`: Human-readable error message (only in error responses)
- `code`: Machine-readable error code (only in error responses)

### Tag JSON Output

**Example command:**
```bash
tagger tag --version 1.2.3 --release-branch main --format=json
```

**Success response:**
```json
{
  "status": "success",
  "data": {
    "tag": "1.2.3"
  }
}
```

**Error response:**
```json
{
  "status": "error",
  "error": "Failed to create tag",
  "code": "TAG_ERROR"
}
```

**Fields:**
- `status`: `"success"` or `"error"`
- `data.tag`: The tag name that was created
- `error`: Human-readable error message (only in error responses)
- `code`: Machine-readable error code (only in error responses)

### Error Codes

- `CONFIGURATION_ERROR`: Invalid configuration or repository state (e.g., detached HEAD)
- `TAG_ERROR`: Failed to create or push tag

### CI Integration Examples

**Extract version in GitHub Actions:**
```yaml
- name: Calculate version
  id: version
  run: |
    VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')
    echo "version=$VERSION" >> $GITHUB_OUTPUT

- name: Use version
  run: echo "Building version ${{ steps.version.outputs.version }}"
```

**Extract version in bash:**
```bash
# Get version
VERSION=$(tagger calculate-version --format=json 2>/dev/null | jq -r '.data.version')

# Check if snapshot
IS_SNAPSHOT=$(tagger calculate-version --format=json 2>/dev/null | jq -r '.data.snapshot')

if [ "$IS_SNAPSHOT" = "true" ]; then
  echo "This is a snapshot build"
fi
```

**Error handling:**
```bash
OUTPUT=$(tagger calculate-version --format=json 2>&1)
STATUS=$(echo "$OUTPUT" | jq -r '.status')

if [ "$STATUS" = "error" ]; then
  ERROR_MSG=$(echo "$OUTPUT" | jq -r '.error')
  ERROR_CODE=$(echo "$OUTPUT" | jq -r '.code')
  echo "Error ($ERROR_CODE): $ERROR_MSG"
  exit 1
fi
```

### Help

For a full listing of the available options in the program, please use the built-in help command.

```bash
tagger --help
```
