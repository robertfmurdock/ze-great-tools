package com.zegreatrob.tools.digger

class MessageDigger(
    majorRegex: Regex = Regex("\\[major]"),
    minorRegex: Regex = Regex("\\[minor]"),
    patchRegex: Regex = Regex("\\[patch]"),
    noneRegex: Regex = Regex("\\[none]"),
) {
    private val regexes = mapOf(
        "major" to majorRegex,
        "minor" to minorRegex,
        "patch" to patchRegex,
        "none" to noneRegex,
    )

    private val allSemverRegex = regexes.map { (group, regex) ->
        "(?<$group>$regex)?"
    }.joinToString("")

    private val allRegex = Regex(
        pattern = "$allSemverRegex(?>\\[(?<storyId>.*?)])?(?>-(?<ease>[1-5])-)?(?>Co-authored-by: .* <(?<coAuthors>.*)>)?",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )

    fun digIntoMessage(input: String): MessageDigResult = allRegex.findAll(input).messageDigResult()
}

private fun Sequence<MatchResult>.messageDigResult() = MessageDigResult(
    storyId = firstNotNullOfOrNull { it.groups["storyId"] }?.value,
    ease = firstNotNullOfOrNull { it.groups["ease"] }?.value?.toIntOrNull(),
    coauthors = mapNotNull { it.groups["coAuthors"]?.value }.toList(),
    semver = mapNotNull {
        when {
            it.groupMatches("major") -> SemverType.Major
            it.groupMatches("minor") -> SemverType.Minor
            it.groupMatches("patch") -> SemverType.Patch
            it.groupMatches("none") -> SemverType.None
            else -> null
        }
    }.sorted()
        .lastOrNull(),

//    mapNotNull { it.groups["semver"]?.value }.map { it.capitalized().let(SemverType::valueOf) }
//        .highestPrioritySemver(),

)

private fun MatchResult.groupMatches(groupName: String) =
    runCatching { this@groupMatches.groups[groupName] }.getOrNull() != null

private fun Sequence<SemverType>.highestPrioritySemver() = sorted().lastOrNull()

enum class SemverType {
    None, Patch, Minor, Major
}
