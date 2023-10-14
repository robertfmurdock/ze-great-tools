package com.zegreatrob.tools.tagger

import org.ajoberstar.grgit.BranchStatus
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.BranchStatusOp
import org.ajoberstar.grgit.operation.LogOp
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream

open class CalculateVersion : DefaultTask(), TaggerExtensionSyntax {

    @Input
    override lateinit var taggerExtension: TaggerExtension

    @Input
    var exportToGithubEnv: Boolean = false

    @TaskAction
    fun execute() {
        logger.quiet(taggerExtension.version)
        val githubEnvFile = System.getenv("GITHUB_ENV")
        if (exportToGithubEnv && githubEnvFile != null) {
            FileOutputStream(githubEnvFile, true)
                .write("TAGGER_VERSION=${taggerExtension.version}".toByteArray())
        }
    }
}

fun Grgit.calculateNextVersion(implicitPatch: Boolean, versionRegex: VersionRegex, releaseBranch: String): String {
    val description = describe {}
    val descriptionComponents = description?.split("-")
    val previousVersionNumber = descriptionComponents?.getOrNull(0)
    if (previousVersionNumber?.length == 0 || previousVersionNumber == null) {
        return "0.0.0"
    }
    val incrementComponent = findAppropriateIncrement(previousVersionNumber, implicitPatch, versionRegex)
    val currentVersionNumber = (
        incrementComponent?.increment(previousVersionNumber.asSemverComponents())
            ?: previousVersionNumber
        )

    return if (canRelease(releaseBranch) && currentVersionNumber != previousVersionNumber) {
        currentVersionNumber
    } else {
        "$currentVersionNumber-SNAPSHOT"
    }
}

private fun Grgit.findAppropriateIncrement(
    previousVersionNumber: String,
    implicitPatch: Boolean,
    minorRegex: VersionRegex,
): ChangeType? =
    log(fun(it: LogOp) {
        it.range(previousVersionNumber, "HEAD")
    })
        .also { if (it.isEmpty()) return null }
        .map { it.changeType(minorRegex) ?: if (implicitPatch) ChangeType.Patch else null }
        .fold(null, ::highestPriority)
        ?: if (implicitPatch) ChangeType.Patch else ChangeType.None

private fun highestPriority(left: ChangeType?, right: ChangeType?) = when {
    left == null -> right
    right == null -> left
    left.priority > right.priority -> left
    else -> right
}

private fun Commit.changeType(versionRegex: VersionRegex) = versionRegex.changeType(shortMessage.lowercase())

enum class ChangeType(val priority: Int) {
    Major(3) {
        override fun increment(components: List<String>): String {
            val (major) = components
            return "${major.toInt() + 1}.0.0"
        }
    },
    Minor(2) {
        override fun increment(components: List<String>): String {
            val (major, minor) = components
            return "$major.${minor.toInt() + 1}.0"
        }
    },
    Patch(1) {
        override fun increment(components: List<String>): String {
            val (major, minor, patch) = components
            return "$major.$minor.${patch.toInt() + 1}"
        }
    },
    None(0) {
        override fun increment(components: List<String>): String {
            val (major, minor, patch) = components
            return "$major.$minor.${patch.toInt()}"
        }
    }, ;

    abstract fun increment(components: List<String>): String
}

private fun String.asSemverComponents() = (
    if (startsWith("v")) {
        substring(1)
    } else {
        this
    }
    ).split(".")

fun Grgit.canRelease(releaseBranch: String): Boolean {
    val currentBranch = branch.current()

    val currentBranchStatus: BranchStatus? = runCatching {
        branch.status(fun(it: BranchStatusOp) {
            it.name = currentBranch.name
        })
    }
        .getOrNull()
    return if (currentBranchStatus == null) {
        false
    } else {
        status().isClean &&
            currentBranchStatus.aheadCount == 0 &&
            currentBranchStatus.behindCount == 0 &&
            currentBranch.name == releaseBranch
    }
}

fun Grgit.tagReport() = tag.list()
    .filter { it.dateTime != null }
    .groupBy { tag ->
        "${tag.dateTime?.year} Week ${tag.weekNumber()}"
    }.toSortedMap()
    .map {
        "${it.key} has ${it.value.size} tags [${it.value.joinToString { tag -> tag.name }}]"
    }
    .joinToString("\n")

private fun Tag.weekNumber() = "${(dateTime?.dayOfYear ?: 0) / 7}".let {
    if (it.length == 1) {
        "0$it"
    } else {
        it
    }
}
