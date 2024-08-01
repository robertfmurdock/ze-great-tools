package com.zegreatrob.tools.digger.core

@Suppress("RegExpRedundantEscape")
class MessageDigger(
    majorRegex: Regex = Regex("\\[major\\]"),
    minorRegex: Regex = Regex("\\[minor\\]"),
    patchRegex: Regex = Regex("\\[patch\\]"),
    noneRegex: Regex = Regex("\\[none\\]"),
) {
    private val regexes =
        mapOf(
            "major" to majorRegex,
            "minor" to minorRegex,
            "patch" to patchRegex,
            "none" to noneRegex,
        )

    private val allSemverRegex =
        regexes.map { (group, regex) ->
            "(?<$group>${regex.cleaned()})?"
        }.joinToString("")

    private val storyIdRegex = "(\\[(?<storyId>.*?)\\])?"
    private val easeRegex = "(-(?<ease>[1-5])-)?"
    private val coAuthorRegex = "(Co-authored-by: .* <(?<coAuthors>.*)>)?"

    private val allRegex = Regex(
        pattern = "$allSemverRegex$storyIdRegex$easeRegex$coAuthorRegex",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )

    fun digIntoMessage(input: String): MessageDigResult = allRegex.findAll(input).messageDigResult()
}

private fun Regex.cleaned(): String {
    val first = pattern.firstOrNull()
    val last = pattern.lastIndexOf("/")
    return if (first == '/') {
        pattern.substring(1, last)
    } else {
        pattern
    }
}

private fun Sequence<MatchResult>.messageDigResult() =
    MessageDigResult(
        coauthors = mapNotNull { it.groups["coAuthors"]?.value }.toList(),
        semver = mapNotNull { getSemver(it) }.maxOrNull(),
        storyId = firstNotNullOfOrNull { it.groups["storyId"] }?.value,
        ease = firstNotNullOfOrNull { it.groups["ease"] }?.value?.toIntOrNull(),
    )

private fun getSemver(it: MatchResult) =
    when {
        it.groupMatches("major") -> SemverType.Major
        it.groupMatches("minor") -> SemverType.Minor
        it.groupMatches("patch") -> SemverType.Patch
        it.groupMatches("none") -> SemverType.None
        else -> null
    }

private fun MatchResult.groupMatches(groupName: String) =
    runCatching { this@groupMatches.groups[groupName] }
        .getOrNull() != null

fun List<SemverType>.highestPrioritySemver() = maxOrNull()
