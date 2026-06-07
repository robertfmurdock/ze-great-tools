Tagger calculates semantic versions from Git history and enforces the tagging policy.
Version numbers live on Git tags. Commit content determines the next version.

Typical build script usage:

```bash
VERSION=$(tagger calculate-version)
./your-build-script.sh version=$VERSION
tagger tag --version $VERSION
```

Use `--format=json` for machine-readable output. Build with the calculated version before tagging.
For fit assessment and philosophy: tagger guide
