package com.zegreatrob.tools.adapter.git

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RunProcessJvmTest {

    @Test
    fun executesCommandSuccessfully() = setup(object {
        val args = listOf("git", "--version")
    }) exercise {
        runProcess(args = args, workingDirectory = ".")
    } verify { result ->
        result.contains("git version").assertIsEqualTo(true)
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
}
