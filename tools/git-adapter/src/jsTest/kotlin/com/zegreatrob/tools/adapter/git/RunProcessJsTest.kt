package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RunProcessJsTest {

    @Test
    fun executesCommandSuccessfully() = setup(object {
        val args = listOf("node", "-e", "console.log('safe-execution')")
    }) exercise {
        runProcess(args = args, workingDirectory = ".")
    } verify { result ->
        result.trim().assertIsEqualTo("safe-execution")
    }

    @Test
    fun failsWhenArgsIsEmpty() = setup(object {
        val args = emptyList<String>()
    }) exercise {
        assertFailsWith<IllegalArgumentException> {
            runProcess(args = args, workingDirectory = ".")
        }
    } verify { exception ->
        exception.message.assertIsEqualTo("Cannot run process with empty arguments list")
    }

    @Test
    fun failsWhenProgramIsBlank() = setup(object {
        val args = listOf("   ", "arg1")
    }) exercise {
        assertFailsWith<IllegalArgumentException> {
            runProcess(args = args, workingDirectory = ".")
        }
    } verify { exception ->
        exception.message.assertIsEqualTo("Executable program name cannot be blank")
    }

    @Test
    fun passesEnvironmentVariables() = setup(object {
        val args = listOf("node", "-e", "console.log(process.env.CUSTOM_TEST_VAR)")
        val env = mapOf("CUSTOM_TEST_VAR" to "custom_value_123")
    }) exercise {
        runProcess(args = args, workingDirectory = ".", env = env)
    } verify { result ->
        result.trim().assertIsEqualTo("custom_value_123")
    }

    @Test
    fun throwsProcessErrorOnNonZeroExit() = setup(object {
        val args = listOf("node", "-e", "process.stderr.write('err'); process.exit(42)")
    }) exercise {
        assertFailsWith<ProcessError> {
            runProcess(args = args, workingDirectory = ".")
        }
    } verify { error ->
        error.exitCode.assertIsEqualTo(42)
        error.stderr.trim().assertIsEqualTo("err")
    }
}
