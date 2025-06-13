package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import kotlinx.serialization.encodeToString

class GenerateSettingsFile : CliktCommand(name = "generate-settings-file") {
    private val file by option()

    override fun run() {
        val prettyJsonFormatter = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val jsonString = prettyJsonFormatter.encodeToString(runtimeDefaultConfig)

        if (file == null) {
            echo(jsonString)
        } else {
            val fileName = file?.ifBlank { ".tagger" }
            val pwd = currentContext.readEnvvar("PWD")
            val outputFile = "$pwd/$fileName"
            if (readFromFile(outputFile) != null) {
                throw CliktError("File already exists.")
            } else {
                jsonString.writeToFile(outputFile)
                echo("Saved to $fileName")
            }
        }
    }
}
