# Reduce Tagger functional test runtime

**Work card protocol: 2**

## Goal

Reduce the wall-clock cost of `AdditionalTasksFunctionalTest` while preserving observable coverage. This is a follow-up to the completed [GitHub release retry card](../work_completed/retry-github-release.md).

## Baseline

The local Gradle report after commit `7179654a` recorded 13 cases in 52.2 seconds. Four asset-validation cases accounted for 20.6 seconds, including 13.6 seconds for the empty file tree. `previousVersion` and `commitReport` accounted for about 6 seconds combined. Treat these as one run. Record comparable before and after timings with the same Gradle invocation, cache conditions, and report scope. Separate suite time from end-to-end command time and account for cases moved into the unit suite.

## Approach

- Start with release asset validation. Use `ProjectBuilder` to exercise `ValidateGithubReleaseAssets.execute()` directly for existing files, an empty matching file tree, and missing files. Check that the failure reports every missing path. Fold in the single-missing-file case if the multiple-file case preserves its evidence. Avoid Git setup for these task-action tests.
- Keep a TestKit scenario for the wiring. Invoke `githubReleaseUpload` with missing assets and verify that validation blocks upload. Make the upload otherwise eligible and use a fake `gh` invocation log to prove no upload occurred. The current upload-named tests call validation directly. Preserve the existing successful generated-asset upload and configuration-cache evidence.
- Review `previousVersion` and `commitReport` against the existing Git, core, and task tests. Map each assertion to its behavior owner and move or consolidate coverage where a narrower interface proves the same result. Preserve any distinct plugin wiring or Gradle execution evidence.
- Review the remaining TestKit scenarios for redundant builds, unnecessary Git setup, and duplicate assertions. Keep integrated evidence for generated inputs, configuration-cache reuse, publication ordering and recovery, and dry-run task graph behavior. Treat those as separate contracts when deciding whether to combine or move scenarios.
- Follow [TESTING.md](../context/TESTING.md) and the Testmints skill. Keep setup, the subject action, and observable evidence visible. Check that a plausible broken implementation fails each materially changed assertion. Run targeted checks serially when they share TestKit outputs, then run the full repository check.

## Acceptance evidence

- Record the before and after timings, scenario counts, and where the moved coverage runs. Explain meaningful timing variation or coverage tradeoffs.
- Verify that missing assets block upload at the task entry point and that direct tests preserve validation outcomes. Document the disposition of the version, report, and remaining TestKit scenarios.
- Pass `./gradlew check -q --console=plain` and complete the final audit before moving this card to completed. Semver intent is `[none]` for test-only changes.

## Coverage disposition

- Three `ValidateGithubReleaseAssetsTest` cases in the Tagger plugin unit suite exercise the task action with real files and a filtered file tree, without Git. Multiple missing files subsume the single-file diagnostic and require every absolute path.
- The upload entry-point test uses an enabled release, an explicit non-snapshot version, a missing asset, and a fake `gh` that records invocations. Validation fails, the upload task never runs, and the log is absent. Replacing the validation dependency with an ordering-only edge made the build unexpectedly succeed. Reversible validator faults that rejected existing assets, rejected empty collections, and reported only the first missing path failed all three direct checks. The restored direct suite passed.
- The empty-upload scenario now uses a file tree that matches nothing. It retains the skipped-upload assertion and checks the validator's no-assets diagnostic in the same build.
- Existing Git and core coverage exercises version calculation from tags, and task unit tests cover Git input annotations. The version's quiet output and the report's console delimiters belong to their respective Gradle task actions. One TestKit build invokes both named tasks against the same tagged repository and verifies each task's success and output, preserving registration and execution evidence.
- Kept both generated draft publication outcomes, generated release creation and upload with configuration-cache reuse, publication ordering and recovery, and the Digger/Tagger dry-run graph check. The generated values, second builds, and retry attempts each provide distinct evidence. Git setup remains for the version/report and dry-run fixtures.

## Timing evidence

Both measurements used `/usr/bin/time -p ./gradlew :tools-tests:tagger-plugin-test:functionalTest --tests '*AdditionalTasksFunctionalTest' --no-build-cache -q --console=plain` on the same machine, with warm Gradle dependencies, no outer build-cache reuse, and fresh TestKit fixture directories. Suite time comes from that class's Gradle XML report.

| Run | Functional cases | Suite | Command wall time |
| --- | ---: | ---: | ---: |
| Before, `7179654a` | 13 | 43.50 s | 46.53 s |
| After | 9 | 37.04 s | 39.21 s |

The functional suite saved 6.46 s (15%) and the command saved 7.32 s (16%). The three direct validation cases took 1.45 s in a separate filtered unit run, including ProjectBuilder startup. The four original validation cases took 16.63 s on the fresh baseline, compared with 20.6 s in the historical run. The first TestKit case took 10.37 s before and 11.88 s after, and the new negative upload wiring case took 5.47 s. These one-run measurements include fixture and nested-build startup variation. Version and report went from 4.61 s across two invocations to 2.10 s in one.

## Closeout

The full repository check (`./gradlew check -q --console=plain`) passed. The independent final audit reviewed both test files and confirmed that the follow-up edits removed dependence on merged output positions and preserved cleanup on unexpected upload success. No actionable findings remained. Semver intent is `[none]`.
