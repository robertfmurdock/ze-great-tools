# Resumable Release Recovery (Concept)

## Purpose

Make a partially completed release safe to inspect and complete without manually creating a tag or repeating immutable
registry uploads.

This is a design concept, not a release procedure. It records an important reliability concern discovered while
investigating a partial 3.6.17 release.

## The problem

A release writes to several systems with different mutability guarantees:

- npm packages are immutable by version.
- Gradle Plugin Portal and Maven Central publications are immutable by version.
- Git tags are immutable release identity.
- GitHub releases and assets are mutable enough to support idempotent create, upload, and publish operations.

The existing release process can publish an immutable artifact, fail before tagging, and then fail again when retried
because the registry rejects the already-consumed version. A GitHub release is therefore not currently a reliable signal
that every distribution completed.

## Desired release invariant

For a non-snapshot release, the preferred observable order is:

```text
build and verify pinned source
→ create or verify the Git tag
→ create GitHub draft
→ verify or publish immutable registry outputs
→ upload assets
→ publish the GitHub release
```

The tag identifies the exact source revision for a release; it is not evidence that every distribution completed. A
published GitHub release should mean that all required immutable registry outputs were verified or published from that
source revision.

## Release identity and evidence

A recovery operation needs an explicit identity:

- `releaseVersion`: the intended version.
- `releaseSourceSha`: the exact Git commit that produced it.
- `releaseMode`: `status` or `resume`.

The source checkout must resolve to `releaseSourceSha` before build or mutation. The build then produces the expected
outputs from that source. Remote output can have one of four states:

| State | Meaning | Resume behavior |
|---|---|---|
| `missing` | The output is absent. | Publish it. |
| `verified` | The output exists and exactly matches expected evidence. | Skip it. |
| `conflict` | The output exists but differs from expected evidence. | Fail before mutation. |
| `unverifiable` | The output exists but cannot be proven to match. | Fail before mutation. |

Only `verified` is safe to skip. Existence alone is never sufficient evidence.

## Evidence by distribution

The preferred evidence is a comparison between the output rebuilt from the pinned source and the output retrieved from
the registry:

- npm: compare the locally built publishable tarball with registry integrity or content.
- Maven Central and Gradle Plugin Portal: compare every published module, marker publication, metadata file, and
  checksum required by the release contract.
- Git tag: require the tag to resolve to `releaseSourceSha`.
- GitHub release assets: compare checksums, not only asset names.

If a registry does not expose enough data to make that comparison, the output is `unverifiable`. Recovery must stop;
it must not create a tag for an ambiguous release.

## Rebuild caveat

Resume rebuilds artifacts from the tagged source; it does not recover binaries from a failed runner. That only works
when the artifact's stable content can be reproduced or otherwise verified. Identical source can still yield different
bytes because of archive timestamps and file ordering, generated metadata, toolchain drift, dependency resolution drift,
platform differences, or signing.

The release design therefore needs durable evidence from the initial release phase, such as source SHA, artifact
coordinates, checksums of reproducible payloads, and toolchain and dependency inputs. Signed artifacts need separate
treatment because their signatures are intentionally nondeterministic even when their unsigned payloads match.

Until an output type has proven reproducibility or equivalent stable evidence, a rebuilt artifact cannot verify a
published one. Its recovery status must be `unverifiable`.

## Execution model

Gradle should own the release state machine. GitHub Actions should provide checkout, credentials, inputs, a run summary,
and diagnostic artifact upload only.

```text
releaseStatus
  → build expected outputs
  → inspect all remote outputs
  → write machine-readable status and concise report

releaseResume
  → run releaseStatus
  → fail on conflict or unverifiable output
  → publish only missing immutable outputs
  → draft release, assets, publish
```

The normal release workflow should invoke resume behavior. GitHub's **Re-run failed jobs** experience then retains the
workflow revision and source commit for future releases. A separate dispatch workflow is necessary for legacy releases;
it must check out the supplied SHA rather than current `main`.

## Version-floor escape hatch

An incomplete version with `conflict` or `unverifiable` registry output must remain untagged. The safe exit is a
successor release with an explicit version floor above the consumed version. Without that explicit floor, normal version
calculation remains unchanged.

For the partial 3.6.17 release, recovery must use
`a4bf6ce9be98ccaade4bb8b47d42e3d55bc292cb`. If its existing immutable outputs cannot be verified against that source,
3.6.17 must not be tagged; a verified successor is required instead.

This legacy case is deliberately stricter than the preferred future flow: a missing historical tag cannot establish
source identity for artifacts that were already published.

## Design constraints

- Do not alter published artifact contents or consumer-facing behavior solely to support recovery.
- Do not use registry existence as provenance.
- Do not let finalizers or broad parallel task graphs hide partial progress.
- Keep each release phase explicit and testable.
- Verify the actual composite-build execution graph with functional marker tasks; dry-run output alone is insufficient
  proof of cross-build ordering.

## Open implementation questions

1. Which publication files form the complete Maven Central and Plugin Portal contract for each module?
2. Are all relevant build outputs reproducible enough for byte-level comparison? If not, which stable registry checksums
   or metadata can supply equivalent evidence?
3. Which release evidence can be recorded without changing published artifact contents?
4. What single Gradle orchestration boundary can sequence root and included-build publication phases without relying on
   cross-build finalizer behavior?
5. How should status reports be serialized and retained as GitHub Actions artifacts?
6. Which functional test fixture can prove missing, verified, conflict, and unverifiable states without touching public
   registries?
