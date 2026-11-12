# Scorecard hardening

**Work card protocol: 2**

## Goal

Reduce GitHub Actions supply-chain risk while preserving direct pushes to main and the existing release and maintenance flows.

## Baseline and decisions

- Initial CLI baseline was 4.6/10. Code-Review was 0/10 and Branch-Protection was 3/10. Review-related deductions are accepted for the trunk workflow. Build remains the only required check.
- The [first hosted run](https://github.com/robertfmurdock/ze-great-tools/actions/runs/43305542964) succeeded. Its verified SARIF contained 30 findings and occupied 18,504 bytes with five-day retention. Scorecard remains advisory, with public API publishing disabled.
- Initial hosted findings included workflow-wide contents write access, missing permission defaults, mutable action references, the floating npm install, and dependency advisories.

## Changes

- Build workflow defaults to contents read. The existing build-and-release job retains contents write for releases and ID-token write for npm provenance. Signing and Sonatype credentials are scoped to the Gradle release step.
- Gradle Update defaults to contents read and retains its PAT and SSH authentication. Update Gradle Wrapper defaults to contents read and grants its maintenance job contents and pull-requests write for the existing GitHub token PR step. Both retain PAT-based automerge.
- Pins by entry point:
  - Build: checkout, setup-java, setup-gradle, setup-node, setup-testspace, upload-artifact, coupling-contribution-action.
  - Gradle Update: checkout, setup-java, setup-gradle, create-pull-request.
  - Update Gradle Wrapper: checkout, setup-java, setup-gradle, wrapper-validation, create-pull-request.
  - GitHub Action test: checkout. Root composite action: setup-node.

| Action | Existing tag | Resolved commit |
| --- | --- | --- |
| actions/checkout | v7 | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| actions/setup-java | v5 | `b6effb05e454b25005698d916606bdc6ffcbf961` |
| actions/setup-node | v7 | `949feb2413d6458794dcd2491c4babbbce0c15c1` |
| actions/upload-artifact | v7 | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` |
| gradle/actions/setup-gradle and wrapper-validation | v6 | `8da46b539cd8fe6ab27ca75128919a18d757604c` |
| peter-evans/create-pull-request | v8 | `3ffccacd46053766916f087ce41d627c0b8cf2d1` |
| robertfmurdock/coupling-contribution-action | v4 | `c8981f90e56de0e69a4cc23808d61a168054ce7c` |
| testspace-com/setup-testspace | v1 | `5fc620521eddeba83c1c0dd58f5a524adf474c93` |

Dependency advisories and the floating npm installation are deferred. Job-level write access remains necessary for the existing release and wrapper PR workflows.

## Resume note

After landing on main, verify Build and the next maintenance runs that have changes to submit. Verify signing and npm provenance on the next normal release. Dispatch Scorecard and record the before-and-after findings, including any remaining job-level write findings. Move this card to work_completed once hosted acceptance evidence is recorded.
