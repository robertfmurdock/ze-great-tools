# Gradle Convention Plugin Constraints

## Sonatype Publishing Configuration

### Constraint: Inline afterEvaluate Configuration Required

**DO NOT** extract `afterEvaluate` publication configuration into top-level functions in `publish.gradle.kts`.

**Working pattern:**
```kotlin
afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach {
        with(it) {
            pom.name.set(project.name)
            pom.licenses { /* inline */ }
            // ... all config inline
        }
    }
}
```

**Broken pattern:**
```kotlin
afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach { configurePom(it) }
}

fun configurePom(publication: MavenPublication) {
    publication.pom.apply {
        name.set(project.name)  // ⚠️ This causes Sonatype 403 errors
    }
}
```

### Why This Matters

**Incident**: Commit d149003a (June 5, 2026)
- Refactored publish.gradle.kts to extract helper functions
- Result: `initializeSonatypeStagingRepository` failed with HTTP 403
- Sonatype returned HTML error page (WAF/proxy-level rejection)
- Credentials were valid (worked in other repos)
- Revert fixed issue immediately

**Root Cause**: Unclear, but likely related to:
- Gradle configuration phase evaluation order
- `project` reference semantics in top-level vs inline functions
- gradle-nexus-publish-plugin interaction with convention plugins

### Evidence

- **POM files**: Identical between working and broken versions
- **Credentials**: Validated - work in testmints/jsmints repos
- **Revert**: Immediate success after reverting refactoring
- **Error type**: HTTP 403 with HTML (not JSON), suggesting malformed request

### Prevention

Until root cause is fully understood:
1. Keep publication configuration inline in `afterEvaluate`
2. Do not extract into top-level helper functions
3. If refactoring needed, extract logic WITHIN the `with(it)` block scope

### Related

- Commit: d149003a (broken), 4b9bfafb (revert)
- Plugin: io.github.gradle-nexus.publish-plugin:2.0.0
- Full analysis: `/PUBLISH_ISSUE_ANALYSIS.md`
