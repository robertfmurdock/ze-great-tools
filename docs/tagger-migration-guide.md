# Tagger Migration Guide

This guide documents breaking changes and how to migrate between major versions of the Tagger plugin and CLI.

## Version 2.x → 3.x (Next Major Release)

### Breaking Changes

#### Removed: `disableDetached` Configuration Property

The deprecated `disableDetached` property has been removed from all configuration interfaces.

**Impact:**
- Gradle plugin DSL: `tagger { disableDetached.set(...) }`
- `.tagger` configuration file: `"disableDetached": true/false`
- CLI flag: `--disable-detached`

**Migration:**
Replace all uses of `disableDetached` with `allowDetachedHead` using inverted logic:

| Old Configuration | New Configuration |
|------------------|------------------|
| `disableDetached = true` (default) | `allowDetachedHead = false` (default) |
| `disableDetached = false` | `allowDetachedHead = true` |

**Examples:**

**Gradle Plugin DSL:**
```kotlin
// Before
tagger {
    releaseBranch = "main"
    disableDetached.set(false)  // Allow detached HEAD
}

// After
tagger {
    releaseBranch = "main"
    allowDetachedHead = true    // Allow detached HEAD
}
```

**Configuration File (`.tagger`):**
```json
// Before
{
  "releaseBranch": "main",
  "disableDetached": false
}

// After
{
  "releaseBranch": "main",
  "allowDetachedHead": true
}
```

**CLI:**
```bash
# Before
tagger calculate-version --disable-detached=false

# After
tagger calculate-version --allow-detached-head=true
```

### Why This Change?

The `disableDetached` property used negative/double-negative logic that was confusing:
- `disableDetached = true` meant "disable the detached HEAD check" (allowing detached HEAD)
- `disableDetached = false` meant "don't disable the check" (blocking detached HEAD)

The new `allowDetachedHead` property uses clearer positive logic:
- `allowDetachedHead = true` explicitly allows detached HEAD state
- `allowDetachedHead = false` (default) blocks detached HEAD state

### Recommended Approach

Most projects should not need to set this property at all. The default behavior (blocking detached HEAD) is correct for most CI/CD pipelines. If you're currently using `disableDetached = false`, ensure your CI checkout step properly tracks a branch rather than relying on this configuration.

See [tagger-detached-head.md](./tagger-detached-head.md) for guidance on fixing detached HEAD in CI.
