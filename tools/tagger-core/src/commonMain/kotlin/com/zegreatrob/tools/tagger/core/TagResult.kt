package com.zegreatrob.tools.tagger.core

sealed class TagResult {
    data object Success : TagResult()
    data class Warning(val message: String) : TagResult()
}
