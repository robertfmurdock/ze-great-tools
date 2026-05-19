package com.zegreatrob.tools.digger.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class SuccessResponse(
    val status: String,
    val data: JsonElement,
)

@Serializable
data class ErrorResponse(
    val status: String,
    val error: String,
    val code: String,
)

fun successResponse(data: JsonElement): String = Json.encodeToString(SuccessResponse.serializer(), SuccessResponse(status = "success", data = data))

fun errorResponse(message: String, code: String): String = Json.encodeToString(ErrorResponse.serializer(), ErrorResponse(status = "error", error = message, code = code))
