package com.zegreatrob.tools.tagger

fun String.asSemverComponents(): List<Int> = stripPrefix()
    .split(".")
    .map(String::toInt)

private fun String.stripPrefix(): String {
    val startIndex = indexOfFirst { it.digitToIntOrNull() != null }
    return substring(startIndex)
}
