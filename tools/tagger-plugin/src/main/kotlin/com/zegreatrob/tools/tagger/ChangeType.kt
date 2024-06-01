package com.zegreatrob.tools.tagger

fun VersionRegex.changeType(message: String) =
    when {
        unified?.matches(message) == true -> findMatchType(message, unified)
        major.matches(message) -> ChangeType.Major
        minor.matches(message) -> ChangeType.Minor
        patch.matches(message) -> ChangeType.Patch
        none.matches(message) -> ChangeType.None
        else -> null
    }

private fun findMatchType(
    message: String,
    regex: Regex,
): ChangeType? {
    val groups = regex.matchEntire(message)?.groups
    return when {
        (groups.groupExists("major")) -> ChangeType.Major
        (groups.groupExists("minor")) -> ChangeType.Minor
        (groups.groupExists("patch")) -> ChangeType.Patch
        (groups.groupExists("none")) -> ChangeType.None
        else -> null
    }
}

private fun MatchGroupCollection?.groupExists(groupName: String): Boolean =
    runCatching { this?.get(groupName) != null }
        .getOrDefault(false)
