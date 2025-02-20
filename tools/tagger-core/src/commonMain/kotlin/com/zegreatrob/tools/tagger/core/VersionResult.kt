package com.zegreatrob.tools.tagger.core

sealed interface VersionResult {

    data class Success(
        val version: String,
        val snapshotReasons: List<SnapshotReason> = emptyList(),
    ) : VersionResult

    data class Failure(val reasons: List<FailureVersionReasons>) : VersionResult
}
