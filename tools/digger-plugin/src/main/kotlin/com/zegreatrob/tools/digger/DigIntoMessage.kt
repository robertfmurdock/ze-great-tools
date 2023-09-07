package com.zegreatrob.tools.digger

import org.gradle.configurationcache.extensions.capitalized

private val allRegex = Regex(
    pattern = "(?>\\[(?<semver>none|patch|minor|major)])?(?>\\[(?<storyId>.*?)])?(?>-(?<ease>[1-5])-)?(?>Co-authored-by: .* <(?<coAuthors>.*)>)?",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
)

fun digIntoMessage(input: String): MessageDigResult = allRegex.findAll(input).messageDigResult()

private fun Sequence<MatchResult>.messageDigResult() = MessageDigResult(
    storyId = firstNotNullOfOrNull { it.groups["storyId"] }?.value,
    ease = firstNotNullOfOrNull { it.groups["ease"] }?.value?.toIntOrNull(),
    coauthors = mapNotNull { it.groups["coAuthors"]?.value }.toList(),
    semver = mapNotNull { it.groups["semver"]?.value }.firstOrNull()
        ?.capitalized()
        ?.let { SemverType.valueOf(it) },
)

enum class SemverType {
    None, Patch, Minor, Major
}
