package com.zegreatrob.tools.tagger.core

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.GitStatus
import com.zegreatrob.tools.adapter.git.TagRef
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun TaggerCore.calculateNextVersion(
    implicitPatch: Boolean,
    versionRegex: VersionRegex,
    releaseBranch: String,
): VersionResult {
    val (previousVersionNumber, lastTagDescription) = lastVersionAndTag()
        ?: return VersionResult("0.0.0")

    val incrementComponent = findAppropriateIncrement(adapter, lastTagDescription, implicitPatch, versionRegex)
    val currentVersionNumber = (
        incrementComponent?.increment(previousVersionNumber.asSemverComponents())
            ?: previousVersionNumber
        )

    val reasonsToUseSnapshot = snapshotReasons(releaseBranch, currentVersionNumber, previousVersionNumber)
    return if (reasonsToUseSnapshot.isEmpty()) {
        VersionResult(currentVersionNumber)
    } else {
        VersionResult("$currentVersionNumber-SNAPSHOT", reasonsToUseSnapshot)
    }
}

data class VersionResult(
    val version: String,
    val snapshotReasons: List<SnapshotReason> = emptyList(),
)

private fun TaggerCore.snapshotReasons(
    releaseBranch: String,
    currentVersionNumber: String,
    previousVersionNumber: String,
) = StatusCheck(
    gitStatus = adapter.status(),
    releaseBranch = releaseBranch,
    currentVersionNumber = currentVersionNumber,
    previousVersionNumber = previousVersionNumber,
)
    .let { statusCheck -> SnapshotReason.entries.filter { reason -> reason.reasonIsValid(statusCheck) } }

private fun findAppropriateIncrement(
    gitAdapter: GitAdapter,
    previousTag: String,
    implicitPatch: Boolean,
    minorRegex: VersionRegex,
): ChangeType? = gitAdapter.logWithRange("HEAD", "^$previousTag")
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

data class StatusCheck(
    val gitStatus: GitStatus,
    val releaseBranch: String,
    val currentVersionNumber: String,
    val previousVersionNumber: String,
)

enum class SnapshotReason {
    DIRTY {
        override fun StatusCheck.exists() = !gitStatus.isClean
    },
    AHEAD {
        override fun StatusCheck.exists() = gitStatus.ahead != 0
    },
    BEHIND {
        override fun StatusCheck.exists() = gitStatus.behind != 0
    },
    NOT_RELEASE_BRANCH {
        override fun StatusCheck.exists() = gitStatus.head != releaseBranch
    },
    NO_NEW_VERSION {
        override fun StatusCheck.exists() = currentVersionNumber == previousVersionNumber
    }, ;

    abstract fun StatusCheck.exists(): Boolean
    fun reasonIsValid(check: StatusCheck): Boolean = check.exists()
}

fun TaggerCore.tagReport() = adapter.listTags()
    .groupBy { tag ->
        "${tag.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).year} Week ${tag.weekNumber()}"
    }
    .toList()
    .sortedBy { (key) -> key }
    .joinToString("\n") { (key, value) ->
        "$key has ${value.size} tags [${value.joinToString { tag -> tag.name }}]"
    }

private fun TagRef.weekNumber() = "${dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear / 7}".let {
    if (it.length == 1) {
        "0$it"
    } else {
        it
    }
}

fun VersionRegex.changeType(message: String): ChangeType? = when {
    unified?.containsMatchIn(message) == true -> findMatchType(message, unified)
    major.containsMatchIn(message) -> ChangeType.Major
    minor.containsMatchIn(message) -> ChangeType.Minor
    patch.containsMatchIn(message) -> ChangeType.Patch
    none.containsMatchIn(message) -> ChangeType.None
    else -> null
}

private fun findMatchType(
    message: String,
    regex: Regex,
): ChangeType? {
    val groups = regex.matchEntire(message)?.groups
    return when {
        (groups.groupExists("major")) -> ChangeType.Major
        (groups.groupExists("minor")) -> ChangeType.Minor
        (groups.groupExists("patch")) -> ChangeType.Patch
        (groups.groupExists("none")) -> ChangeType.None
        else -> null
    }
}

private fun MatchGroupCollection?.groupExists(groupName: String): Boolean = runCatching { this?.get(groupName) != null }
    .getOrDefault(false)
