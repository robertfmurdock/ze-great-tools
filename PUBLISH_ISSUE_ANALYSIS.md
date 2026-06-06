# Sonatype 403 Error Analysis

## Summary

Refactoring the `publish.gradle.kts` convention plugin to extract top-level functions caused Sonatype's `initializeSonatypeStagingRepository` task to fail with a 403 error.

## Evidence

### What Failed
- **Commit**: d149003a (refactoring convention plugins)
- **Error**: `Failed to create staging repository, server at https://ossrh-staging-api.central.sonatype.com/service/local/ responded with status code 403`
- **Response**: HTML page (not JSON), suggesting WAF/proxy-level rejection

### What Worked
- **Revert**: 4b9bfafb (reverting the refactor) - succeeded immediately
- **Other repos**: testmints and jsmints published successfully with same credentials on same day
- **Previous version**: 3.0.6 published successfully on June 4th

## Investigation Findings

### POM Files are Identical
Generated POM files from both versions are byte-for-byte identical:
- Same group ID
- Same project names
- Same dependencies
- Same metadata

### The Refactoring Change

**Before (working):**
```kotlin
afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach {
        with(it) {
            val scmUrl = "https://github.com/robertfmurdock/ze-great-tools"
            pom.name.set(project.name)
            // ... more config inline
        }
    }
}
```

**After (broken):**
```kotlin
afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach { configurePom(it) }
}

fun configurePom(publication: MavenPublication) {
    val scmUrl = "https://github.com/robertfmurdock/ze-great-tools"
    publication.pom.apply {
        name.set(project.name)
        // ... config via extracted functions
    }
}
```

## Hypothesis

The 403 error is NOT related to POM content (which is identical). It must be related to:

1. **Gradle configuration phase timing** - extracting top-level functions may change when/how the plugin configuration is evaluated
2. **nexus-publish-plugin interaction** - the plugin might inspect project configuration at initialization time, and the refactored code causes it to see something invalid
3. **HTTP request formation** - somehow the refactored code causes the nexus-publish-plugin to construct a malformed HTTP request to Sonatype

### Why HTML 403?

The fact that Sonatype returned an HTML error page (not JSON API error) suggests:
- Request blocked at WAF/proxy level before reaching the API
- Possibly malformed request headers or body
- NOT an authentication/authorization issue (credentials are valid)

## Root Cause (Theory)

**The `project` reference in top-level functions may have different semantics than `project` in inline code within `with(it)` blocks.**

In Gradle precompiled script plugins:
- Inline code within `with()` blocks has clear receiver context
- Top-level functions might capture `project` at plugin application time, not at `afterEvaluate` time
- This could cause the nexus-publish-plugin to read incorrect project state during its initialization

However, this doesn't fully explain WHY it would cause a 403 specifically...

## Recommended Actions

1. **Keep the revert** - the working version is proven safe
2. **File issue with gradle-nexus-publish-plugin** - this could be a bug in how they interact with convention plugins
3. **Add integration test** - test that publishing configuration works correctly across all subprojects
4. **Document the constraint** - "Do not extract afterEvaluate logic into top-level functions in publish.gradle.kts"

## Questions Remaining

1. What specifically in the HTTP request caused Sonatype to return 403?
2. Why does the nexus-publish-plugin behavior change based on function extraction?
3. Is this a Gradle bug, plugin bug, or just an undocumented constraint?

## Testing Ideas

- [ ] Add logging to see project names during configuration
- [ ] Compare HTTP requests between working and broken versions (requires network capture)
- [ ] Try making functions explicit `Project` extension functions
- [ ] Test with different Gradle/plugin versions
- [ ] Check if other nexus-publish-plugin users have hit this

## Detection Strategy

Since this issue only manifests during actual Sonatype API interaction (not during POM generation), it's difficult to test in isolation. Instead:

1. **Manual verification**: Before merging publish.gradle.kts changes, manually test:
   ```bash
   ./gradlew :tools:initializeSonatypeStagingRepository -Pversion=test --stacktrace
   ```
   (Requires SONATYPE_USERNAME and SONATYPE_PASSWORD env vars)

2. **CI integration**: The release workflow already catches this (it failed for 3.1.1)

3. **Code review**: Flag any refactoring of `afterEvaluate` blocks in publish.gradle.kts for extra scrutiny

## Prevention

1. Document constraint in `agents.d/context/GRADLE_PLUGIN_CONSTRAINTS.md`
2. Add comment in publish.gradle.kts warning about this issue
3. Consider filing issue with gradle-nexus-publish-plugin maintainers
