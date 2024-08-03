package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.json.ContributionParser.parseContribution
import com.zegreatrob.tools.digger.json.ContributionParser.parseContributions
import java.io.FileOutputStream

fun initializeGitRepo(
    projectDirectoryPath: String,
    addFileNames: Set<String>,
    commits: List<String> = listOf(),
    initialTag: String? = null,
): org.ajoberstar.grgit.Grgit {
    val grgit = org.ajoberstar.grgit.Grgit.init(mapOf("dir" to projectDirectoryPath))
    disableGpgSign(projectDirectoryPath)
    if (addFileNames.isNotEmpty()) {
        grgit.add(
            fun org.ajoberstar.grgit.operation.AddOp.() {
                patterns = addFileNames
            },
        )
    }
    if (initialTag != null) {
        grgit.addTag(initialTag)
    }
    commits.forEach { message -> grgit.addCommitWithMessage(message) }

    grgit.remote.add(
        fun org.ajoberstar.grgit.operation.RemoteAddOp.() {
            this.name = "origin"
            this.url = projectDirectoryPath
        },
    )
    grgit.checkout(
        fun org.ajoberstar.grgit.operation.CheckoutOp.() {
            branch = "main"
            createBranch = true
        },
    )
    grgit.pull()
    grgit.branch.change(
        fun org.ajoberstar.grgit.operation.BranchChangeOp.() {
            this.name = "main"
            this.startPoint = "origin/main"
            this.mode = org.ajoberstar.grgit.operation.BranchChangeOp.Mode.TRACK
        },
    )
    return grgit
}

fun org.ajoberstar.grgit.Grgit.addTag(initialTag: String?): org.ajoberstar.grgit.Tag? = tag.add(
    fun(it: org.ajoberstar.grgit.operation.TagAddOp) {
        it.name = initialTag
    },
)

fun org.ajoberstar.grgit.Grgit.addCommitWithMessage(message: String): org.ajoberstar.grgit.Commit =
    commit(
        fun(it: org.ajoberstar.grgit.operation.CommitOp) {
            it.author = org.ajoberstar.grgit.Person("Funky Testerson", "funk@test.io")
            it.committer = org.ajoberstar.grgit.Person("Testy Funkerson", "test@funk.edu")
            it.message = message
        },
    )

private fun disableGpgSign(projectDir: String) {
    FileOutputStream("$projectDir/.git/config", true)
        .writer().use {
            it.write("[commit]\n        gpgsign = false")
        }
}

fun parseCurrentAuthors(output: String) = parseContribution(output)?.authors

fun parseSemver(output: String) = parseContribution(output)?.semver

fun parseStoryId(output: String) = parseContribution(output)?.storyId

fun parseAll(output: String) = parseContributions(output)
