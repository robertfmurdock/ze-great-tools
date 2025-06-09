package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

interface GenerateSettingsFileTestSpec {

    fun execute(): TestResult

    @Test
    fun willGenerateSettingsFileToStandardOutByDefault() {
        val result = execute()

        val json = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        assertEquals(TestResult.Success(json.encodeToString(runtimeDefaultConfig)), result)
    }
}
