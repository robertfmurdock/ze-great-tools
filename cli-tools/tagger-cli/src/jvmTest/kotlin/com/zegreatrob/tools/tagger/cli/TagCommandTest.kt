package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest

class TagCommandTest : TagTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "tag")
    }

    override fun configureWithDefaults() {
        arguments += "--release-branch=master"
        arguments += projectDir.absolutePath
    }

    override fun configureWithOverrides(
        releaseBranch: String?,
        userName: String?,
        userEmail: String?,
        warningsAsErrors: Boolean?,
    ) {
        if (releaseBranch != null) {
            arguments += "--release-branch=$releaseBranch"
        }
        if (userName != null) {
            arguments += "--user-name=$userName"
        }
        if (userEmail != null) {
            arguments += "--user-email=$userEmail"
        }
        if (warningsAsErrors != null) {
            arguments += "--warnings-as-errors=$warningsAsErrors"
        }
        arguments += projectDir.absolutePath
    }

    override fun execute(version: String): TestResult {
        arguments += "--version=$version"
        val test = cli()
            .test(arguments)
        return if (test.statusCode == 0) {
            test
                .output
                .trim()
                .let { TestResult.Success(it) }
        } else {
            TestResult.Failure(test.output.trim())
        }
    }
}
