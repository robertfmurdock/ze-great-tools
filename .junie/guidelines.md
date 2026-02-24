# AI Agent Guidelines

All AI agents working on this repository must follow these rules:

1. **Verification Before Submission**: Before calling the `submit` tool, run `./gradlew check` for the entire project. Build cache is fine unless caching seems suspicious.
2. **TestMints Pattern**: Use [TestMints](https://github.com/robertfmurdock/testmints) with the anonymous-object setup pattern (see `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt`).
3. **Setup / Exercise / Verify Discipline**:
   - Setup object holds stable data and constructed objects; keep short or low-interest values inline if it reads better.
   - Keep SUT calls out of setup; setup should only build inputs and environment.
   - Pre-req checks belong in setup (e.g., `?: error("...")`), not verify.
   - Use `setup { ... }` callbacks only for required steps that cannot live in the setup object (see `tools/tagger-plugin/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`).
   - Exercise focuses on the core subject of the test, usually a single call (or a tight cluster of related calls).
   - Verify handles all lookups and validation, including simple ones like `findByName`.
   - When a test needs cleanup, use `verifyAnd { ... } teardown { ... }` together so teardown always runs and stays visually paired with verification (see `tools-tests/tagger-plugin-test/src/functionalTest/kotlin/com/zegreatrob/tools/tagger/AdditionalTasksFunctionalTest.kt`).
   - When exercise produces both stdout and file output, return a small result object so verify can assert both cleanly.
4. **Assertions & Expectations**:
   - Prefer `assertIsEqualTo` with data objects for clearer diffs.
   - In verify, prefer assert-style checks; reserve `error(...)` for setup-only pre-reqs.
   - Expectation data in `verify` is fine; if used once, inline it inside the assertion with reasonable line breaks.
   - If the expectation is used more than once, define a variable in `verify`.
5. **Test Names**: Keep names unique, brief, and scenario-focused.
6. **Java Toolchain**: This project uses Java Toolchain 21. Ensure any new modules or environment checks respect this version.
7. **Efficient Verification**: For multi-test refactors, batch edits per file or logical cluster, then run targeted compile tasks (e.g., `:module:compileKotlinJvm` / `:compileKotlinJs`) after each batch. Run `./gradlew :tools-tests:check` once per file or at the end for full confidence. Prefer quieter Gradle output (`--quiet` or `--console=plain`) when possible.
