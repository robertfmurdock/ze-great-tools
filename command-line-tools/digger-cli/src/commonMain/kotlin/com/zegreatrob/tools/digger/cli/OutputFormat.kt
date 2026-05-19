package com.zegreatrob.tools.digger.cli

enum class OutputFormat {
    TEXT,
    JSON,
    ;

    companion object {
        fun fromString(value: String): OutputFormat = when (value.lowercase()) {
            "text" -> TEXT

            "json" -> JSON

            else -> throw IllegalArgumentException(
                "Invalid format '$value'. Must be one of: ${entries.joinToString(", ") { it.name.lowercase() }}",
            )
        }
    }
}
