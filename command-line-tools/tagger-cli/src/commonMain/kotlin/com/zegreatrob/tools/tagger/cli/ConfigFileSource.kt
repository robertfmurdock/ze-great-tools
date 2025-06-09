package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ValueSource
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.tagger.json.TaggerConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

class ConfigFileSource(val envvarReader: (key: String) -> String?) : ValueSource {
    override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> {
        val configAsElement = readConfigFileAsJsonElement()
            ?: return emptyList()
        return findInvocations(configAsElement, option)
    }

    private fun findInvocations(
        configAsElement: JsonElement,
        option: Option,
    ): List<ValueSource.Invocation> {
        var cursor: JsonElement? = configAsElement
        val parts = option.parts()
        for (part in parts) {
            if (cursor !is JsonObject) return emptyList()
            cursor = cursor[part]
        }
        if (cursor == null) return emptyList()

        try {
            if (cursor is JsonArray) {
                return cursor.map {
                    ValueSource.Invocation.value(it.jsonPrimitive.content)
                }
            }
            return ValueSource.Invocation.just(cursor.jsonPrimitive.content)
        } catch (_: IllegalArgumentException) {
            return emptyList()
        }
    }

    private fun Option.parts(): List<String> = valueSourceKey?.split(".")
        ?: listOf(ValueSource.name(this).kebabToCamelCase())

    private fun String.kebabToCamelCase(): String {
        val pattern = "-[a-z]".toRegex()
        return replace(pattern) { it.value.last().uppercase() }
    }

    private fun readConfigFileAsJsonElement(): JsonElement? {
        val pwd = envvarReader("PWD")
        val fileContents = readFromFile("$pwd/.tagger")
            ?: return null

        val config = Json.decodeFromString<TaggerConfig>(fileContents)
        val configAsElement = Json.encodeToJsonElement(config)
        return configAsElement
    }
}
