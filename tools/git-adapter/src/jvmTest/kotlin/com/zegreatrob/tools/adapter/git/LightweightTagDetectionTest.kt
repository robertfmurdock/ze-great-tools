package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class LightweightTagDetectionTest {

    @field:TempDir
    lateinit var projectDir: File

    @Test
    fun `detects lightweight tags when present`() = asyncSetup(object {
        val adapter = GitAdapter(projectDir.absolutePath)
        val tagName = "v1.0.0"
    }) {
        initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "test@example.com")
        }
        runProcess(listOf("git", "tag", tagName), projectDir.absolutePath)
    } exercise {
        adapter.listAllTagNames()
    } verify { result ->
        result
            .assertIsEqualTo(listOf(tagName))
    }

    @Test
    fun `returns empty list when no tags exist`() = asyncSetup(object {
        val adapter = GitAdapter(projectDir.absolutePath)
    }) {
        initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "test@example.com")
        }
    } exercise {
        adapter.listAllTagNames()
    } verify { result ->
        result
            .assertIsEqualTo(emptyList())
    }

    @Test
    fun `detects both annotated and lightweight tags`() = asyncSetup(object {
        val adapter = GitAdapter(projectDir.absolutePath)
        val annotatedTag = "v1.0.0"
        val lightweightTag = "v2.0.0"
    }) {
        initializeGitRepo(
            directory = projectDir.absolutePath,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "test@example.com")
            newAnnotatedTag(annotatedTag, "HEAD", null, null)
        }
        runProcess(listOf("git", "tag", lightweightTag), projectDir.absolutePath)
    } exercise {
        adapter.listAllTagNames()
            .sorted()
    } verify { result ->
        result
            .assertIsEqualTo(listOf(annotatedTag, lightweightTag))
    }
}
