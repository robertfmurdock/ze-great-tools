package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class ProcessErrorTest {

    @Test
    fun `formats Azure DevOps permission error with remediation guidance`() = setup(object {
        val exitCode = 128
        val stderr = """
            remote: TF401027: You need the Git 'GenericContribute' permission to perform this action.
            fatal: unable to access 'https://dev.azure.com/org/project/_git/repo': The requested URL returned error: 403
        """.trimIndent()
        val command = "git push --tags"
    }) exercise {
        ProcessError(exitCode, stderr, command)
            .toUserMessage()
    } verify { result ->
        result.contains("Command failed: git push --tags (exit code 128)")
            .assertIsEqualTo(true)
        result.contains("Error output:")
            .assertIsEqualTo(true)
        result.contains("TF401027")
            .assertIsEqualTo(true)
        result.contains("Azure DevOps")
            .assertIsEqualTo(true)
        result.contains("Contribute")
            .assertIsEqualTo(true)
        result.contains("Create tag")
            .assertIsEqualTo(true)
    }

    @Test
    fun `formats GitHub Actions permission error with remediation guidance`() = setup(object {
        val exitCode = 403
        val stderr = """
            remote: Permission to user/repo.git denied to github-actions[bot].
            fatal: unable to access 'https://github.com/user/repo.git/': The requested URL returned error: 403
        """.trimIndent()
        val command = "git push --tags"
    }) exercise {
        ProcessError(exitCode, stderr, command)
            .toUserMessage()
    } verify { result ->
        result.contains("Command failed: git push --tags (exit code 403)")
            .assertIsEqualTo(true)
        result.contains("Error output:")
            .assertIsEqualTo(true)
        result.contains("Permission to user/repo.git denied")
            .assertIsEqualTo(true)
        result.contains("GitHub Actions")
            .assertIsEqualTo(true)
        result.contains("permissions: contents: write")
            .assertIsEqualTo(true)
    }

    @Test
    fun `detects permission errors from exit codes`() = setup(object {
        val error128 = ProcessError(128, "some error", "git push")
        val error403 = ProcessError(403, "some error", "git push")
        val error1 = ProcessError(1, "some error", "git push")
    }) exercise {
        object {
            val is128Permission = error128.isPermissionError
            val is403Permission = error403.isPermissionError
            val is1Permission = error1.isPermissionError
        }
    } verify { result ->
        result.is128Permission
            .assertIsEqualTo(true)
        result.is403Permission
            .assertIsEqualTo(true)
        result.is1Permission
            .assertIsEqualTo(false)
    }

    @Test
    fun `detects permission errors from stderr content`() = setup(object {
        val error = ProcessError(1, "Permission denied", "git push")
    }) exercise {
        error.isPermissionError
    } verify { result ->
        result
            .assertIsEqualTo(true)
    }
}
