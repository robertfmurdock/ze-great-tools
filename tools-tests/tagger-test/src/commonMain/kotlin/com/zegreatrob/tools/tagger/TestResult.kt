package com.zegreatrob.tools.tagger

sealed class TestResult {
    data class Success(val message: String, val details: String = "", val warnings: List<String> = emptyList()) : TestResult()
    data class Failure(val reason: String) : TestResult()
}
