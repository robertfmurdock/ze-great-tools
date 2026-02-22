# AI Agent Guidelines

All AI agents working on this repository must follow these rules:

1. **Verification Before Submission**: Before calling the `submit` tool, run `./gradlew check` for the entire project. Build cache is fine unless caching seems suspicious.
2. **TestMints Pattern**: When adding or converting tests, use [TestMints](https://github.com/robertfmurdock/testmints) with the anonymous-object setup pattern (see `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt`).
3. **Setup / Exercise / Verify Discipline**:
   - Construct objects in `setup`.
   - Put the single subject action in `exercise`.
   - Put all assertions and lookups in `verify`, including simple ones like `findByName`.
   - If setup needs side effects not referenced later, use the `setup { ... }` callback (see `tools/tagger-plugin/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`).
4. **Java Toolchain**: This project uses Java Toolchain 21. Ensure any new modules or environment checks respect this version.
5. **Efficient Verification**: For multi-test refactors, batch edits per file or logical cluster, then run targeted compile tasks (e.g., `:module:compileKotlinJvm` / `:compileKotlinJs`) after each batch. Run `./gradlew :tools-tests:check` once per file or at the end for full confidence. Prefer quieter Gradle output (`--quiet` or `--console=plain`) when possible.
