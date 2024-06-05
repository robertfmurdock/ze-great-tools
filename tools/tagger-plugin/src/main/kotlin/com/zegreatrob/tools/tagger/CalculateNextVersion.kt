package com.zegreatrob.tools.tagger

import org.ajoberstar.grgit.BranchStatus
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.BranchStatusOp
import org.ajoberstar.grgit.operation.LogOp

fun Grgit.calculateNextVersion(
    lastTagDescription: String,
    implicitPatch: Boolean,
    versionRegex: VersionRegex,
    previousVersionNumber: String,
    releaseBranch: String,
): String {
    val incrementComponent = findAppropriateIncrement(lastTagDescription, implicitPatch, versionRegex)
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
    previousTag: String,
    implicitPatch: Boolean,
    minorRegex: VersionRegex,
): ChangeType? =
    log(
        fun(it: LogOp) {
            it.range(previousTag, "HEAD")
        },
    )
        .also { if (it.isEmpty()) return null }
        .map { it.changeType(minorRegex) ?: if (implicitPatch) ChangeType.Patch else null }
        .fold(null, ::highestPriority)
        ?: if (implicitPatch) ChangeType.Patch else ChangeType.None

private fun highestPriority(
    left: ChangeType?,
    right: ChangeType?,
) = when {
    left == null -> right
    right == null -> left
    left.priority > right.priority -> left
    else -> right
}

private fun Commit.changeType(versionRegex: VersionRegex) = versionRegex.changeType(shortMessage.lowercase())

enum class ChangeType(val priority: Int) {
    Major(3) {
        override fun increment(components: List<Int>): String {
            val (major) = components
            return "${major + 1}.0.0"
        }
    },
    Minor(2) {
        override fun increment(components: List<Int>): String {
            val (major, minor) = components
            return "$major.${minor + 1}.0"
        }
    },
    Patch(1) {
        override fun increment(components: List<Int>): String {
            val (major, minor, patch) = components
            return "$major.$minor.${patch + 1}"
        }
    },
    None(0) {
        override fun increment(components: List<Int>): String {
            val (major, minor, patch) = components
            return "$major.$minor.$patch"
        }
    }, ;

    abstract fun increment(components: List<Int>): String
}

fun Grgit.canRelease(releaseBranch: String): Boolean {
    val currentBranch = branch.current()

    val currentBranchStatus: BranchStatus? =
        runCatching {
            branch.status(
                fun(it: BranchStatusOp) {
                    it.name = currentBranch.name
                },
            )
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

fun Grgit.tagReport() =
    tag.list()
        .filter { it.dateTime != null }
        .groupBy { tag ->
            "${tag.dateTime?.year} Week ${tag.weekNumber()}"
        }.toSortedMap()
        .map {
            "${it.key} has ${it.value.size} tags [${it.value.joinToString { tag -> tag.name }}]"
        }
        .joinToString("\n")

private fun Tag.weekNumber() =
    "${(dateTime?.dayOfYear ?: 0) / 7}".let {
        if (it.length == 1) {
            "0$it"
        } else {
            it
        }
    }
