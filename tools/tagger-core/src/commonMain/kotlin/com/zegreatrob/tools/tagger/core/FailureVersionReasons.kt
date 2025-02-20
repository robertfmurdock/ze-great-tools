package com.zegreatrob.tools.tagger.core

enum class FailureVersionReasons(val message: String) {
    NoRemote("repository has no remote."),
    NoTagsExist(
        """repository has no tags.
If this is a new repository, use `tag` to set the initial version.""",
    ),
}
