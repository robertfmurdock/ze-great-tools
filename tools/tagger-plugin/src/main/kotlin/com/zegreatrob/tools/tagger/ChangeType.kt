package com.zegreatrob.tools.tagger

fun VersionRegex.changeType(message: String) = when {
    major.matches(message) -> ChangeType.Major
    minor.matches(message) -> ChangeType.Minor
    patch.matches(message) -> ChangeType.Patch
    none.matches(message) -> ChangeType.None
    else -> null
}
