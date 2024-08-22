package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.GitStatus
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag

fun calculateNextVersion(
    adapter: GitAdapter,
    lastTagDescription: String,
    implicitPatch: Boolean,
    versionRegex: VersionRegex,
    previousVersionNumber: String,
    releaseBranch: String,
): String {
    val incrementComponent = findAppropriateIncrement(adapter, lastTagDescription, implicitPatch, versionRegex)
    val currentVersionNumber = (
        incrementComponent?.increment(previousVersionNumber.asSemverComponents())
            ?: previousVersionNumber
        )

    return if (adapter.status().canRelease(releaseBranch) && currentVersionNumber != previousVersionNumber) {
        currentVersionNumber
    } else {
        "$currentVersionNumber-SNAPSHOT"
    }
}

private fun findAppropriateIncrement(
    gitAdapter: GitAdapter,
    previousTag: String,
    implicitPatch: Boolean,
    minorRegex: VersionRegex,
): ChangeType? =
    gitAdapter.logWithRange("HEAD", "^$previousTag")
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

private fun CommitRef.changeType(versionRegex: VersionRegex) = versionRegex.changeType(fullMessage.trim())

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

fun GitStatus.canRelease(releaseBranch: String): Boolean =
    this.isClean &&
        this.ahead == 0 &&
        this.behind == 0 &&
        this.head == releaseBranch

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
