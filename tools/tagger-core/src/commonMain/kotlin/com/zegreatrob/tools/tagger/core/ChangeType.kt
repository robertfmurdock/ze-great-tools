package com.zegreatrob.tools.tagger.core

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.GitStatus
import com.zegreatrob.tools.adapter.git.TagRef
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun TaggerCore.calculateNextVersion(
    implicitPatch: Boolean,
    disableDetached: Boolean,
    versionRegex: VersionRegex,
    forceSnapshot: Boolean,
    releaseBranch: String,
): VersionResult {
    val gitStatus = this.adapter.status()
    if (disableDetached && gitStatus.upstream.isEmpty()) {
        return VersionResult.Failure(listOf(FailureVersionReasons.NoRemote))
    }
    val (previousVersionNumber, lastTagDescription) = lastVersionAndTag()
        ?: run {
            val allTagNames = adapter.listAllTagNames()
            return if (allTagNames.isNotEmpty()) {
                VersionResult.Failure(emptyList(), lightweightTagsFound(allTagNames))
            } else {
                VersionResult.Failure(listOf(FailureVersionReasons.NoTagsExist))
            }
        }
    val previousVersionComponents = previousVersionNumber.asSemverComponents()
        ?: return VersionResult.Failure(listOf(FailureVersionReasons.VersionMissingElements))

    val incrementComponent = findAppropriateIncrement(adapter, lastTagDescription, implicitPatch, versionRegex)
    val currentVersionNumber = incrementComponent?.increment(previousVersionComponents) ?: previousVersionNumber

    val reasonsToUseSnapshot = snapshotReasons(
        releaseBranch,
        currentVersionNumber,
        previousVersionNumber,
        gitStatus,
        forceSnapshot,
    )

    val shouldSnapshot = reasonsToUseSnapshot.isNotEmpty()

    val warnings = buildList {
        if (!disableDetached && gitStatus.upstream.isEmpty() && gitStatus.head == releaseBranch) {
            add("⚠️  Running with allowDetachedHead on release branch. Without upstream tracking, stable version may trigger unintended releases.")
        }
    }

    return VersionResult.Success(
        if (!shouldSnapshot) currentVersionNumber else "$currentVersionNumber-SNAPSHOT",
        reasonsToUseSnapshot,
        warnings,
    )
}

private fun snapshotReasons(
    releaseBranch: String,
    currentVersionNumber: String,
    previousVersionNumber: String,
    gitStatus: GitStatus,
    forceSnapshot: Boolean,
) = StatusCheck(
    gitStatus = gitStatus,
    releaseBranch = releaseBranch,
    currentVersionNumber = currentVersionNumber,
    previousVersionNumber = previousVersionNumber,
    forceSnapshot = forceSnapshot,
).let { statusCheck -> SnapshotReason.entries.filter { reason -> reason.reasonIsValid(statusCheck) } }

private fun findAppropriateIncrement(
    gitAdapter: GitAdapter,
    previousTag: String,
    implicitPatch: Boolean,
    minorRegex: VersionRegex,
): ChangeType? {
    val commits = gitAdapter.logWithRange("HEAD", "^$previousTag")
    if (commits.isEmpty()) return null

    val changeTypes = commits.map { it.changeType(minorRegex) ?: if (implicitPatch) ChangeType.Patch else null }
    val highest = changeTypes.fold(null, ::highestPriority)
    return highest ?: if (implicitPatch) ChangeType.Patch else ChangeType.None
}

private fun highestPriority(
    left: ChangeType?,
    right: ChangeType?,
) = when {
    left == null -> right
    right == null -> left
    left.priority > right.priority -> left
    else -> right
}

private fun CommitRef.changeType(versionRegex: VersionRegex) = versionRegex.changeType(subject())

private fun CommitRef.subject(): String = fullMessage.lineSequence().firstOrNull()?.trim() ?: ""

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
    val forceSnapshot: Boolean,
)

enum class SnapshotReason(val message: String) {
    FORCED("Snapshot forced via --force-snapshot flag.") {
        override fun StatusCheck.exists() = forceSnapshot
    },
    DIRTY("Uncommitted changes in working directory. Commit or stash before tagging.") {
        override fun StatusCheck.exists() = !gitStatus.isClean
    },
    AHEAD("Local branch ahead of remote. Push changes before tagging.") {
        override fun StatusCheck.exists() = gitStatus.ahead != 0
    },
    BEHIND("Local branch behind remote. Pull changes before tagging.") {
        override fun StatusCheck.exists() = gitStatus.behind != 0
    },
    NOT_RELEASE_BRANCH("Not on configured release branch. Switch to release branch before tagging.") {
        override fun StatusCheck.exists() = gitStatus.head != releaseBranch
    },
    NO_NEW_VERSION("No new commits since last tag. Version unchanged.") {
        override fun StatusCheck.exists() = currentVersionNumber == previousVersionNumber
    }, ;

    abstract fun StatusCheck.exists(): Boolean
    fun reasonIsValid(check: StatusCheck): Boolean = check.exists()
}

fun TaggerCore.tagReport() = adapter.listTags().groupBy { tag ->
    "${tag.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).year} Week ${tag.weekNumber()}"
}.toList().sortedBy { (key) -> key }.joinToString("\n") { (key, value) ->
    "$key has ${value.size} tags [${value.joinToString { tag -> tag.name }}]"
}

private fun TagRef.weekNumber() = (dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear / 7)
    .toString()
    .padWeekNumber()

private fun String.padWeekNumber() = if (length == 1) "0$this" else this

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

private fun MatchGroupCollection?.groupExists(groupName: String): Boolean = runCatching { this?.get(groupName) != null }.getOrDefault(false)
