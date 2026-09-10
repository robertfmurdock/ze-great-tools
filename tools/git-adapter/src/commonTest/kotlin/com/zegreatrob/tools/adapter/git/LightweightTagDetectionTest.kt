package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.async.asyncSetup
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LightweightTagDetectionTest {

    private lateinit var projectDir: String

    @BeforeTest
    fun setup() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun teardown() {
        removeDirectory(projectDir)
    }

    @Test
    fun detectsLightweightTagsWhenPresent() = asyncSetup(object {
        val adapter = GitAdapter(projectDir)
        val tagName = "v1.0.0"
    }) {
        initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "test@example.com")
        }
        runProcess(listOf("git", "tag", tagName), projectDir)
    } exercise {
        adapter.listAllTagNames()
    } verify { result ->
        result.assertIsEqualTo(listOf(tagName))
    }

    @Test
    fun returnsEmptyListWhenNoTagsExist() = asyncSetup(object {
        val adapter = GitAdapter(projectDir)
    }) {
        initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "test@example.com")
        }
    } exercise {
        adapter.listAllTagNames()
    } verify { result ->
        result.assertIsEqualTo(emptyList())
    }

    @Test
    fun detectsBothAnnotatedAndLightweightTags() = asyncSetup(object {
        val adapter = GitAdapter(projectDir)
        val annotatedTag = "v1.0.0"
        val lightweightTag = "v2.0.0"
    }) {
        initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test")
            config("user.email", "test@example.com")
            newAnnotatedTag(annotatedTag, "HEAD", null, null)
        }
        runProcess(listOf("git", "tag", lightweightTag), projectDir)
    } exercise {
        adapter.listAllTagNames().sorted()
    } verify { result ->
        result.assertIsEqualTo(listOf(annotatedTag, lightweightTag))
    }
}
