# Task: Add Azure DevOps pipeline documentation

## Goal
Create comprehensive Azure DevOps pipeline documentation for tagger, eliminating the multi-iteration setup friction reported in issue #310.

## Background
Issue #310 documents a painful ADO pipeline integration experience with multiple failed runs due to:
- Missing documentation for ADO-specific setup (only GitHub Actions examples exist)
- Unclear permission requirements
- Detached HEAD handling not documented for ADO
- Bootstrap workflow not clear
- Integration patterns scattered across error messages

A complete ADO example would have collapsed multiple debugging iterations into one successful setup.

## Hard constraints
- Documentation must reflect actual working patterns (test before documenting)
- Examples must be copy-paste ready for common cases
- Must cover permission setup explicitly (highest friction point)
- Keep examples evergreen (avoid version-specific ADO syntax where possible)

## What to change

### 1. Create docs/tagger-ado-pipeline.md

**Structure:**
1. Prerequisites
2. Complete working pipeline example
3. Permission configuration (with screenshots or detailed steps)
4. Bootstrap workflow (first tag setup)
5. Troubleshooting common issues
6. Advanced patterns

**Content to include:**

#### Prerequisites section
- ADO pipeline must use `checkout: self` with specific settings
- Node.js task configuration
- First annotated tag must exist before pipeline runs
- Build service identity needs specific permissions

#### Complete working pipeline
```yaml
trigger:
  - main  # or your release branch

pool:
  name: <pool-name>  # or vmImage: ubuntu-latest

steps:
  - checkout: self
    persistCredentials: true   # Required: tagger needs to push the tag back
    fetchDepth: 0              # Required: tagger needs full history
    fetchTags: true            # Required: tagger needs previous annotated tags

  - script: |
      git checkout -B main HEAD   # Attach detached HEAD to release branch
    displayName: 'Attach HEAD to release branch'

  - task: NodeTool@0
    inputs:
      versionSpec: '20.x'

  # Your build/test steps here
  # - script: npm ci
  # - script: npm run build
  # - script: npm test

  - script: |
      set -euo pipefail
      VERSION=$(npx tagger -q calculate-version \
                  --release-branch=main \
                  --allow-detached-head=true 2>&1 \
                | grep -E '^[0-9]+(\.[0-9]+){2}' \
                | head -1 | sed 's/-SNAPSHOT$//' | xargs)
      npx tagger tag \
        --release-branch=main \
        --version="$VERSION" \
        --allow-detached-head=true \
        --user-name="Azure Pipelines" \
        --user-email="noreply@dev.azure.com"
    displayName: 'Tag successful build'
    condition: succeeded()
```

#### Permission configuration
Document step-by-step (ADO's UI is non-obvious):

**Grant permissions to Build Service identity:**
1. Navigate to Project Settings → Repositories → [Your Repository]
2. Click "Security" tab
3. Search for "[Project Name] Build Service ([Org Name])"
4. Set these permissions to "Allow":
   - Contribute: Allow
   - Create tag: Allow
5. Save changes

**Why this matters:**
- Without Contribute: tag push fails with 403
- Without Create tag: tag creation fails with permission error
- Error manifests as git push failure (see troubleshooting)

#### Bootstrap workflow
Document creating the first tag:

**Before first pipeline run:**
```bash
# Create initial annotated tag
git tag -a 0.0.0 -m "Initial version"
git push origin 0.0.0
```

**Important notes:**
- Must be annotated tag (use `-a`), not lightweight
- Must be pushed to remote before pipeline runs
- Choose starting version: `0.0.0` for brand new, `1.0.0` if already released

**Alternative: Let tagger create it**
If repo has no tags yet:
```bash
npx tagger tag \
  --release-branch=main \
  --version=0.0.0 \
  --user-name="Your Name" \
  --user-email="you@example.com"
```

#### Troubleshooting section

**"remote: TF401027: You need the Git 'GenericContribute' permission"**
- Cause: Build service lacks permissions
- Fix: Follow permission configuration steps above
- See: Permission configuration section

**"repository has no tags"**
- Cause: No annotated tags exist yet
- Fix: Create initial tag per bootstrap workflow
- See: Bootstrap workflow section

**"HEAD is detached" or "HEAD has no upstream tracking branch"**
- Cause: ADO checkout creates detached HEAD by default
- Fix 1: Add `git checkout -B <branch> HEAD` step (shown in example)
- Fix 2: Use `--allow-detached-head=true` flag
- See: Complete working pipeline section

**"found 1 tag (0.0.0) but it is lightweight"**
- Cause: Initial tag created without `-a` flag
- Fix: Recreate as annotated tag:
  ```bash
  git tag -d 0.0.0
  git tag -a 0.0.0 <sha> -m "Initial version"
  git push --force origin 0.0.0
  ```

**Pre-push hook fails (project-specific)**
- Cause: Hook validation runs on tag-only push
- Fix: Add `--no-verify` if hook incorrectly blocks tag-only pushes
- Note: Only use after understanding what hook checks

#### Advanced patterns section

**Multi-branch setup:**
```yaml
trigger:
  - main
  - develop

steps:
  - script: |
      BRANCH=$(git rev-parse --abbrev-ref HEAD)
      if [ "$BRANCH" = "HEAD" ]; then
        BRANCH="${BUILD_SOURCEBRANCHNAME}"
      fi
      git checkout -B "$BRANCH" HEAD
    displayName: 'Attach HEAD to branch'
```

**Conditional tagging (only on main):**
```yaml
- script: |
    # tagging commands here
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
  displayName: 'Tag release'
```

**Using semantic commits for version bumps:**
Reference tagger's version calculation based on conventional commits.

### 2. Update main README

Add ADO link alongside GitHub Actions:

```markdown
## CI Integration

Tagger works with major CI/CD platforms:

- [GitHub Actions](docs/tagger-github-actions.md)
- [Azure DevOps](docs/tagger-ado-pipeline.md)
- Other platforms: see [general CI guidance](docs/tagger-ci.md)
```

### 3. Link from error messages (if not already done)

Ensure error messages link to relevant docs:
- Detached HEAD error → docs/tagger-detached-head.md
- Permission errors → mention checking CI platform docs
- Bootstrap error → link to bootstrap section

### 4. Consider creating docs/tagger-ci.md

General CI guidance applicable to all platforms:
- Why `fetchDepth: 0` is required
- Why annotated tags matter
- Permission requirements (conceptual)
- Detached HEAD explanation
- Link to platform-specific guides

## Checklist

### Documentation creation
- [ ] Create `docs/tagger-ado-pipeline.md` with complete structure
- [ ] Write prerequisites section
- [ ] Write complete working pipeline example (tested)
- [ ] Document permission configuration with step-by-step instructions
- [ ] Document bootstrap workflow with examples
- [ ] Write troubleshooting section covering common errors
- [ ] Add advanced patterns section
- [ ] Consider screenshots for permission configuration (optional)

### Integration with existing docs
- [ ] Update main README with ADO link
- [ ] Ensure error messages link to relevant sections
- [ ] Create or update `docs/tagger-ci.md` for general guidance
- [ ] Ensure `docs/tagger-detached-head.md` exists (prerequisite)
- [ ] Cross-reference between related docs

### Validation
- [ ] Test pipeline example in actual ADO environment
- [ ] Verify permission steps are accurate
- [ ] Test bootstrap workflow
- [ ] Verify troubleshooting solutions work
- [ ] Have someone unfamiliar with tagger try following the docs
- [ ] Move this file to `agents.d/tasks_completed/`

## Definition of done
- Complete ADO pipeline documentation exists
- Documentation is discoverable from README
- Examples are copy-paste ready and tested
- Permission setup is explicit and unambiguous
- Troubleshooting section covers all errors from issue #310
- A developer new to tagger can set up ADO pipeline in one attempt
- Documentation is linked from relevant error messages
