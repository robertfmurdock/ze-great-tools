package com.zegreatrob.tools.tagger.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VersionSuccessResponse(
    val status: String,
    val data: VersionData,
)

@Serializable
data class VersionData(
    val version: String,
    val snapshot: Boolean,
    val snapshotReasons: List<String> = emptyList(),
)

@Serializable
data class ErrorResponse(
    val status: String,
    val error: String,
    val code: String,
)

@Serializable
data class TagSuccessResponse(
    val status: String,
    val data: TagData,
)

@Serializable
data class TagData(
    val tag: String,
)

fun versionSuccessResponse(data: VersionData): String = Json.encodeToString(VersionSuccessResponse.serializer(), VersionSuccessResponse(status = "success", data = data))

fun tagSuccessResponse(data: TagData): String = Json.encodeToString(TagSuccessResponse.serializer(), TagSuccessResponse(status = "success", data = data))

fun errorResponse(message: String, code: String): String = Json.encodeToString(ErrorResponse.serializer(), ErrorResponse(status = "error", error = message, code = code))
