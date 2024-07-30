package com.zegreatrob.tools.digger.core

import kotlinx.datetime.Instant
import java.io.File
import java.nio.charset.Charset

fun DiggerGitWrapper.currentCommitTag(): TagRef? {
    val firstTag = listTags().maxByOrNull { it.dateTime }
    val headCommitId = headCommitId()
    return if (firstTag?.commitId != headCommitId) {
        null
    } else {
        firstTag
    }
}

class DiggerGitWrapper(private val workingDirectory: File) {

    fun headCommitId(): String {
        val process =
            ProcessBuilder(
                listOf(
                    "git",
                    "--no-pager",
                    "rev-parse",
                    "HEAD",
                ),
            )
                .directory(workingDirectory)
                .start()
        val outputText = process.inputStream.readAllBytes().toString(Charset.defaultCharset())
        val error = process.errorStream.readAllBytes().toString(Charset.defaultCharset())
        process.waitFor()
        if (error.isNotEmpty()) {
            throw Error(error)
        }
        return outputText.trim()
    }

    fun listTags(): List<TagRef> {
        val process =
            ProcessBuilder(
                listOf(
                    "git",
                    "--no-pager",
                    "tag",
                    "--list",
                    "--format=%(refname:strip=2),%(*objectname),%(creatordate:iso-strict)",
                ),
            )
                .directory(workingDirectory)
                .start()
        val outputText = process.inputStream.readAllBytes().toString(Charset.defaultCharset())
        val error = process.errorStream.readAllBytes().toString(Charset.defaultCharset())
        process.waitFor()
        val output = outputText.split("\n").mapNotNull {
            val commaSplit = it.split(",")
            if (commaSplit.size >= 3) {
                TagRef(
                    name = commaSplit.subList(0, commaSplit.size - 2).joinToString(""),
                    commitId = commaSplit.reversed()[1],
                    dateTime = Instant.parse(commaSplit.last()),
                )
            } else {
                null
            }
        }
        if (error.isNotEmpty()) {
            throw Error(error)
        }
        return output
    }
}

data class TagRef(val name: String, val commitId: String, val dateTime: Instant)
