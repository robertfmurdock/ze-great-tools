package com.zegreatrob.tools.tagger.core

sealed class TagResult {
    object Success : TagResult()
    data class Error(val message: String) : TagResult()
}
