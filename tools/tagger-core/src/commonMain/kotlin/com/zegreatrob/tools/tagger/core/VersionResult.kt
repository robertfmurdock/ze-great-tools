package com.zegreatrob.tools.tagger.core

sealed interface VersionResult {

    data class Success(
        val version: String,
        val snapshotReasons: List<SnapshotReason> = emptyList(),
    ) : VersionResult

    data class Failure(
        val reasons: List<FailureVersionReasons>,
        val customMessage: String? = null,
    ) : VersionResult {
        val message get() = customMessage ?: "Inappropriate configuration: ${reasons.joinToString("\n") { it.message }}"
    }
}
