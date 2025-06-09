package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import kotlinx.serialization.encodeToString

class GenerateSettingsFile : CliktCommand(name = "generate-settings-file") {
    override fun run() {
        val prettyJsonFormatter = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        echo(prettyJsonFormatter.encodeToString(runtimeDefaultConfig))
    }
}
