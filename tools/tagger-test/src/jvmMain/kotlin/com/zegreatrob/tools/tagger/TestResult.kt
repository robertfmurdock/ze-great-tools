package com.zegreatrob.tools.tagger

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Failure(val reason: String) : TestResult()
}
