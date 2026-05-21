package com.zegreatrob.tools.tagger.core

enum class FailureVersionReasons(val message: String) {
    NoRemote(buildNoUpstreamError()),
    NoTagsExist(
        """repository has no tags.
If this is a new repository, use `tag` to set the initial version.""",
    ),
    VersionMissingElements("the most recent tag did not have all three semver components."),
}

fun lightweightTagsFound(tags: List<String>): String = buildString {
    appendLine("found ${tags.size} tag${if (tags.size == 1) "" else "s"} (${tags.joinToString(", ")}) but ${if (tags.size == 1) "it is" else "they are"} lightweight.")
    appendLine("Tagger requires annotated tags so it can record version metadata.")
    appendLine()
    appendLine("Recreate ${if (tags.size == 1) "it" else "them"} with:")
    tags.forEach { tag ->
        appendLine("  git tag -d $tag")
        appendLine("  git tag -a $tag <sha> -m \"${tag.removePrefix("v")}\"")
        appendLine("  git push --force origin $tag")
        if (tag != tags.last()) appendLine()
    }
}
