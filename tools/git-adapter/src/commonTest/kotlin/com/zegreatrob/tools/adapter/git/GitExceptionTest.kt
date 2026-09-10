package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class GitExceptionTest {

    @Test
    fun parsesNotAGitRepositoryError() = setup(object {
        val command = "git rev-parse HEAD"
        val exitCode = 128
        val stderr = "fatal: not a git repository (or any of the parent directories): .git"
    }) exercise {
        parseGitError(command, exitCode, stderr)
    } verify { result ->
        (result is GitRepositoryNotFoundException).assertIsEqualTo(true)
        result.command.assertIsEqualTo(command)
        result.exitCode.assertIsEqualTo(exitCode)
        result.stderr.assertIsEqualTo(stderr)
    }

    @Test
    fun parsesTagAlreadyExistsError() = setup(object {
        val command = "git tag --annotate --message=v1.0.0 v1.0.0 HEAD"
        val exitCode = 128
        val stderr = "fatal: tag 'v1.0.0' already exists"
    }) exercise {
        parseGitError(command, exitCode, stderr)
    } verify { result ->
        (result is GitTagAlreadyExistsException).assertIsEqualTo(true)
        val tagException = result as GitTagAlreadyExistsException
        tagException.tagName.assertIsEqualTo("v1.0.0")
        tagException.command.assertIsEqualTo(command)
        tagException.exitCode.assertIsEqualTo(exitCode)
    }

    @Test
    fun parsesInvalidRefError() = setup(object {
        val command = "git rev-parse nonexistent-branch"
        val exitCode = 128
        val stderr = "fatal: ambiguous argument 'nonexistent-branch': unknown revision or path not in the working tree."
    }) exercise {
        parseGitError(command, exitCode, stderr)
    } verify { result ->
        (result is GitInvalidRefException).assertIsEqualTo(true)
        val invalidRefException = result as GitInvalidRefException
        invalidRefException.ref.assertIsEqualTo("nonexistent-branch")
        invalidRefException.command.assertIsEqualTo(command)
    }

    @Test
    fun parsesUncommittedChangesError() = setup(object {
        val command = "git checkout main"
        val exitCode = 1
        val stderr = "error: Your local changes to the following files would be overwritten by checkout: file.txt\nPlease commit your changes or stash them before you switch branches."
    }) exercise {
        parseGitError(command, exitCode, stderr)
    } verify { result ->
        (result is GitUncommittedChangesException).assertIsEqualTo(true)
        result.command.assertIsEqualTo(command)
        result.exitCode.assertIsEqualTo(exitCode)
    }

    @Test
    fun parsesGenericCommandExecutionError() = setup(object {
        val command = "git custom-unknown-command"
        val exitCode = 1
        val stderr = "git: 'custom-unknown-command' is not a git command. See 'git --help'."
    }) exercise {
        parseGitError(command, exitCode, stderr)
    } verify { result ->
        (result is GitCommandExecutionException).assertIsEqualTo(true)
        result.command.assertIsEqualTo(command)
        result.exitCode.assertIsEqualTo(exitCode)
    }

    @Test
    fun formatsPermissionRemediationInUserMessage() = setup(object {
        val command = "git push --tags"
        val exitCode = 128
        val stderr = "remote: TF401027: You need the Git 'GenericContribute' permission to perform this action."
    }) exercise {
        parseGitError(command, exitCode, stderr).toUserMessage()
    } verify { message ->
        message.contains("Command failed: git push --tags (exit code 128)").assertIsEqualTo(true)
        message.contains("Azure DevOps").assertIsEqualTo(true)
        message.contains("Contribute").assertIsEqualTo(true)
    }
}
