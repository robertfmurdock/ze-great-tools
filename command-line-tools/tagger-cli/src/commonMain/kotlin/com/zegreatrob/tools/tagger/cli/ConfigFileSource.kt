package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktError
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
        val cursor = option.parts()
            .fold(configAsElement as JsonElement?) { element, part ->
                (element as? JsonObject)?.get(part)
            } ?: return emptyList()

        return invocationsFrom(cursor)
    }

    private fun invocationsFrom(cursor: JsonElement): List<ValueSource.Invocation> = try {
        if (cursor is JsonArray) {
            cursor.map { ValueSource.Invocation.value(it.jsonPrimitive.content) }
        } else {
            ValueSource.Invocation.just(cursor.jsonPrimitive.content)
        }
    } catch (_: IllegalArgumentException) {
        emptyList()
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

        val config = try {
            Json.decodeFromString<TaggerConfig>(fileContents)
        } catch (e: Exception) {
            throw CliktError("Failed to parse .tagger file: ${e.message}")
        }
        val configAsElement = Json.encodeToJsonElement(config)
        return configAsElement
    }
}
