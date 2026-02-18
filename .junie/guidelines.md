# AI Agent Guidelines

To ensure the highest quality and stability of the project, all AI agents working on this repository must follow these rules:

1. **Verification Before Submission**: Before calling the `submit` tool, you MUST run `./gradlew check` on the entire project to ensure that your changes haven't introduced any regressions and that all tests (including functional and multiplatform tests) pass. Using the build cache and not rerunning all tasks is fine, unless there is suspicion that something is wrong with the caching system.
2. **Standardized Testing**: When adding or converting tests, use the [TestMints](https://github.com/robertfmurdock/testmints) library. Follow the anonymous-object setup pattern as demonstrated in `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt`. If the setup requires steps for side effects (e.g., creating resources that aren't directly referenced in the test), use the callback parameter of the `setup` function as shown in `tools/tagger-plugin/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`.
3. **Exercise and Verify**: The `exercise` block should be reserved for the function subject to being tested. All setup and side effects should be performed within the `setup` or its callback.
4. **Java Toolchain**: This project uses Java Toolchain 21. Ensure any new modules or environment checks respect this version.
