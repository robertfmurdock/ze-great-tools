Create and push an annotated Git tag at the specified version.

Tagger uses a two-step workflow to separate version calculation from tagging:

1. Run `tagger calculate-version` to compute the next version and check snapshot conditions.
2. Review the output. If it's a release version (no -SNAPSHOT suffix), use that version here.

The `--version` flag is **required**. Typically, you pass the version from calculate-version output.

You can manually override the calculated version when needed (for example, to correct a versioning mistake or handle an
exceptional release). When overriding, ensure the version adheres to semantic versioning and follows your project's
tagging policy.
