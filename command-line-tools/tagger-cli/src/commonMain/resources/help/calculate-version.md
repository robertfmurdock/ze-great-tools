Calculate the next semantic version from Git history and commit annotations.

When `calculate-version` outputs a version ending in `-SNAPSHOT`, one or more conditions prevent a release version:

| Reason                                                  | Remediation                                                            |
|---------------------------------------------------------|------------------------------------------------------------------------|
| `FORCED` — Snapshot forced via `--force-snapshot` flag  | Remove `--force-snapshot` flag when ready for release                  |
| `DIRTY` — Uncommitted changes in working directory      | Commit or stash changes before tagging                                 |
| `AHEAD` — Local branch ahead of remote                  | Push changes before tagging                                            |
| `BEHIND` — Local branch behind remote                   | Pull changes before tagging                                            |
| `NOT_RELEASE_BRANCH` — Not on configured release branch | Switch to release branch before tagging (default: main)                |
| `NO_NEW_VERSION` — No new commits since last tag        | Add commits with version annotations (`[major]`, `[minor]`, `[patch]`) |
