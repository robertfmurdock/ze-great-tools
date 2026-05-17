package com.zegreatrob.tools.tagger.core

enum class FailureVersionReasons(val message: String) {
    NoRemote("HEAD has no upstream tracking branch. See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md"),
    NoTagsExist(
        """repository has no tags.
If this is a new repository, use `tag` to set the initial version.""",
    ),
    VersionMissingElements("the most recent tag did not have all three semver components."),
}
