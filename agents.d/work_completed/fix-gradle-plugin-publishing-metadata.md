# Fix Gradle Plugin Publishing Metadata

**Work card protocol: 2**

## Goal
Restore Maven Central metadata and Javadoc artifacts for Gradle plugin publications that were lost during convention-plugin consolidation, so all four plugins pass Sonatype validation.

## Constraints and acceptance evidence
- Semver intent: `[patch]` — restore previously working publishing functionality.
- Preserve the publishing/signing configuration consolidation benefits and avoid reintroducing unnecessary duplication.
- Every Maven publication from all four Gradle plugin projects, including marker and implementation publications, needs non-empty POM name, description, URL, license, SCM, and developer metadata. Name/description may use project metadata; they do not need to duplicate the Plugin DSL display name/description.
- All published artifacts, including the `tagger-guide` and `digger-guide` JVM publications, need a Javadoc jar.
- Explain how Plugin DSL declarations relate to marker coordinates/descriptors and identify why metadata is absent from the marker POMs. Compare the convention behavior with the working pre-consolidation setup.
- Publish affected publications to Maven Local and inspect every POM field and Javadoc artifact. If local signing credentials block the publish, use a validation-only signing bypass that does not alter release configuration, or report the specific blocker after verifying generated POMs and artifacts. Follow shared testing and validation guidance; demonstrate behavior regressions before implementation when a suitable automated test can cover them.

## Findings
- The pre-consolidation module scripts registered POM configuration after the `plugins` and `gradlePlugin` blocks. The shared convention registered its original `afterEvaluate` callback during plugin application, early enough to miss marker publications added later by Gradle Plugin Publish. The current shared `configureEach` hook configures publications whenever they are added, including markers.
- Gradle Plugin DSL declarations determine marker coordinates and plugin descriptors; they do not need to supply Maven POM name/description. The acceptance requirement is non-empty Maven metadata on each marker POM.
- `library.gradle.kts` created a Javadoc jar but attached it with `withType { ... }` on existing publications only. KMP's JVM publication is created later, so `configureEach` is required to attach the provider to late publications.
- The Gradle Plugin Publish plugin independently registers marker signing tasks. A temporary init script disabled signing tasks and removed signature-only artifacts for the Maven Local validation invocation; repository release signing configuration was unchanged.
- Testmints tests are not suitable for exercising generated Maven publication artifacts; Gradle publication tasks and artifact inspection are the direct acceptance evidence.

## Progress
- Updated the library convention so its Javadoc jar attaches to publications created later, including KMP JVM publications.
- Published all four plugin projects and both guide JVM publications to Maven Local with a validation-only signing bypass. Inspected all eight plugin POMs (four markers and four implementation publications) and both guide POMs; name, description, URL, license, SCM, and developer fields are present and non-empty.
- Confirmed Maven Local contains Javadoc jars for all four plugin artifacts and both guide JVM artifacts.
- `./gradlew check -q --console=plain` passed. No Testmints test was added because the publication outputs are directly verified through Gradle's generated and installed artifacts.
