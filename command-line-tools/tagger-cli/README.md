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

For complete option documentation and automation guidance, run `tagger calculate-version --help` or `tagger tag --help`.

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

## Common Errors and Troubleshooting

### Lightweight Tags

**Error:** `found N tag(s) (...) but it is/they are lightweight.`

**Cause:** Tagger requires annotated tags (created with `git tag -a`) because they store metadata like tagger name, email, and timestamp. Lightweight tags (created with `git tag <name>`) don't include this information.

**Solution:** Recreate the tag(s) as annotated tags. The error message will provide exact commands, for example:

```bash
git tag -d 1.0.0
git tag -a 1.0.0 <sha> -m "1.0.0"
git push --force origin 1.0.0
```

Replace `<sha>` with the commit hash where the tag should point (often the same commit the lightweight tag pointed to).

### Permission Errors When Pushing Tags

**Error:** `Command failed: git push --tags (exit code 128)` or `exit code 403`

**Common causes:**
- The account running tagger doesn't have push permission on the repository
- CI/CD pipelines often use restricted service accounts by default

**Solutions:**

**For Azure DevOps:**
1. Go to Project Settings → Repositories → Security
2. Find the Build Service identity (e.g., `<Project Name> Build Service`)
3. Grant both `Contribute` and `Create tag` permissions
4. Ensure "Allow scripts to access the OAuth token" is enabled in your pipeline YAML:
   ```yaml
   - checkout: self
     persistCredentials: true
   ```

**For GitHub Actions:**
1. Add write permissions to your workflow job:
   ```yaml
   jobs:
     build:
       permissions:
         contents: write  # Required for pushing tags
   ```
2. Ensure you're not using a read-only `GITHUB_TOKEN`

**For GitLab CI:**
```yaml
variables:
  GIT_STRATEGY: clone
  
before_script:
  - git config --global user.email "ci@example.com"
  - git config --global user.name "CI Bot"
```

### No Tags Exist

**Error:** `repository has no tags.`

**Cause:** This is a new repository or no tags have been created yet.

**Solution:** Create an initial tag manually to establish the version baseline:

```bash
git tag -a 0.1.0 -m "Initial version"
git push origin 0.1.0
```

After this, tagger can calculate subsequent versions automatically.

### Help

For detailed option documentation, snapshot reason explanations, and structured output field definitions, use the built-in help:

```bash
tagger --help
tagger calculate-version --help
tagger tag --help
```
