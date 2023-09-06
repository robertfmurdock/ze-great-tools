package com.zegreatrob.tools.digger

private val allRegex = Regex(
    pattern = "(?>\\[(?<storyId>.*?)])?(?>-(?<ease>[1-5])-)?(?>Co-authored-by: .* <(?<coAuthors>.*)>)?",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
)

fun digIntoMessage(input: String): MessageDigResult = allRegex.findAll(input).messageDigResult()

private fun Sequence<MatchResult>.messageDigResult() = MessageDigResult(
    storyId = firstNotNullOfOrNull { it.groups["storyId"] }?.value,
    ease = firstNotNullOfOrNull { it.groups["ease"] }?.value?.toIntOrNull(),
    coauthors = mapNotNull { it.groups["coAuthors"]?.value }.toList(),
)
