package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GenerateSettingsFile : CliktCommand(name = "generate-settings-file") {
    private val file by option()
    private val merge by option().boolean()

    override fun run() {
        val prettyJsonFormatter = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val defaultConfig = prettyJsonFormatter.encodeToJsonElement(runtimeDefaultConfig)

        if (file == null) {
            echo(prettyJsonFormatter.encodeToString(defaultConfig))
        } else {
            val fileName = file?.ifBlank { ".tagger" }
            val pwd = currentContext.readEnvvar("PWD")
            val outputFile = "$pwd/$fileName"
            val existingFileData = readFromFile(outputFile)
            if (existingFileData != null) {
                if (merge == true) {
                    val originalData = prettyJsonFormatter.parseToJsonElement(existingFileData)
                    val mergeData = mergeJson(defaultConfig, originalData)
                    prettyJsonFormatter.encodeToString(mergeData).writeToFile(outputFile)
                } else {
                    throw CliktError("File already exists.")
                }
            } else {
                prettyJsonFormatter.encodeToString(defaultConfig).writeToFile(outputFile)
                echo("Saved to $fileName")
            }
        }
    }

    private fun mergeJson(
        defaultConfig: JsonElement,
        originalData: JsonElement,
    ): JsonObject {
        val mergeData = buildJsonObject {
            defaultConfig.jsonObject.forEach { (key, value) ->
                val originalValue = originalData.jsonObject[key]
                val hasValue = originalValue?.jsonPrimitive?.contentOrNull != null
                put(
                    key,
                    if (hasValue) {
                        originalValue
                    } else {
                        value
                    },
                )
            }
        }
        return mergeData
    }
}
