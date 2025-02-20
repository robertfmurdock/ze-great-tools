package com.zegreatrob.tools.tagger.core

fun String.asSemverComponents(): List<Int>? = stripPrefix()
    .split(".")
    .map(String::toInt)
    .let { if (it.size == 3) it else null }

private fun String.stripPrefix(): String {
    val startIndex = indexOfFirst { it.digitToIntOrNull() != null }
    return substring(startIndex)
}
