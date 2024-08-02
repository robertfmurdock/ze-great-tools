package com.zegreatrob.tools.digger.core

class MessageDigger(
    majorRegex: Regex = Defaults.majorRegex,
    minorRegex: Regex = Defaults.minorRegex,
    patchRegex: Regex = Defaults.patchRegex,
    noneRegex: Regex = Defaults.noneRegex,
    storyIdRegex: Regex = Defaults.storyIdRegex,
    easeRegex: Regex = Defaults.easeRegex,
) {
    init {
        if (!storyIdRegex.pattern.contains("?<storyId>")) {
            throw RuntimeException("StoryIdRegex must include a storyId group. The regex was: ${storyIdRegex.pattern}")
        }
        if (!easeRegex.pattern.contains("?<ease>")) {
            throw RuntimeException("EaseRegex must include an ease group. The regex was: ${easeRegex.pattern}")
        }
    }

    @Suppress("RegExpRedundantEscape")
    object Defaults {
        val majorRegex = Regex("\\[major\\]")
        val minorRegex = Regex("\\[minor\\]")
        val patchRegex = Regex("\\[patch\\]")
        val noneRegex = Regex("\\[none\\]")
        val storyIdRegex = Regex("\\[(?<storyId>.*?)\\]")
        val easeRegex = Regex("-(?<ease>[1-5])-")
    }

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

    private val cleanedStoryIdRegex = "(${storyIdRegex.cleaned()})?"
    private val cleanedEaseRegex = "(${easeRegex.cleaned()})?"
    private val coAuthorRegex = "(Co-authored-by: .* <(?<coAuthors>.*)>)?"

    private val allRegex = Regex(
        pattern = "$allSemverRegex$cleanedStoryIdRegex$cleanedEaseRegex$coAuthorRegex",
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
