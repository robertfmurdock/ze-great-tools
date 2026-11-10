package com.zegreatrob.tools.dashboard

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class JUnitCountsTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `rejects malformed XML instead of counting a partial report`() = setup(object {
        val report = directory.resolve("truncated.xml").apply { writeText("<testsuite><testcase/>") }
    }) exercise {
        runCatching { countJUnitReport(report) }
    } verify { result ->
        result.exceptionOrNull()?.message.assertIsEqualTo("Invalid JUnit XML in '$report'.")
    }
}
