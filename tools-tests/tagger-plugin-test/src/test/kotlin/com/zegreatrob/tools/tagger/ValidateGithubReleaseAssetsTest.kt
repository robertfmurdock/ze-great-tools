package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class ValidateGithubReleaseAssetsTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun acceptsExistingAssets() = setup(object {
        val task = validationTask()
        val assets = listOf("one.zip", "two.zip").map { name ->
            File(projectDir, name).apply { writeText("asset") }
        }
    }) {
        task.assets.from(assets)
    } exercise {
        runCatching { task.execute() }
    } verify { result ->
        result.exceptionOrNull().assertIsEqualTo(null)
    }

    @Test
    fun acceptsFileTreeWithNoMatches() = setup(object {
        val task = validationTask()
        val directory = File(projectDir, "distributions").apply {
            mkdirs()
            resolve("readme.txt").writeText("not an asset")
        }
        val assets = task.project.fileTree(directory).include("*.zip")
    }) {
        task.assets.from(assets)
    } exercise {
        runCatching { task.execute() }
    } verify { result ->
        result.exceptionOrNull().assertIsEqualTo(null)
    }

    @Test
    fun reportsEveryMissingAsset() = setup(object {
        val task = validationTask()
        val assets = listOf("one.zip", "two.zip", "three.zip").map { File(projectDir, it) }
    }) {
        task.assets.from(assets)
    } exercise {
        runCatching { task.execute() }
    } verify { result ->
        val failure = result.exceptionOrNull()
        (failure is GradleException).assertIsEqualTo(true)
        assets.forEach { asset ->
            failure?.message?.contains(asset.absolutePath).assertIsEqualTo(true, failure?.message ?: "")
        }
    }

    private fun validationTask(): ValidateGithubReleaseAssets {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        return project.tasks.register("validateAssets", ValidateGithubReleaseAssets::class.java).get().apply {
            githubReleaseEnabled.set(true)
        }
    }
}
