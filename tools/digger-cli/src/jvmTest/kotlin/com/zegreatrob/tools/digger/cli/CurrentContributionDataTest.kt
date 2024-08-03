package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.digger.CurrentContributionTestSpec
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CurrentContributionDataTest : CurrentContributionTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    override val addFileNames: Set<String> = emptySet()
    override fun runCurrentContributionData(): String =
        CurrentContributionData()
            .test("--dir ${projectDir.absolutePath}").output
}
