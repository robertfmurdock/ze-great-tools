package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.test.git.disableGpgSign
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.InitOp
import java.io.File
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

interface TagAndPushTestSpec {
    var projectDir: File
    val addFileNames: Set<String>

    fun initializeGitRepo(
        commits: List<String>,
        initialTag: String? = null,
        remoteUrl: String = projectDir.absolutePath,
    ) = com.zegreatrob.tools.test.git.initializeGitRepo(
        directory = projectDir.absolutePath,
        remoteUrl = remoteUrl,
        addFileNames = addFileNames,
        initialTag = initialTag,
        commits = commits,
    )

    fun configureWithDefaults()
    fun execute(): TestResult

    @Test
    fun tagWillTagAndPushSuccessfully() {
        configureWithDefaults()

        val originDirectory = createTempDirectory()
        val originGrgit = Grgit.init(fun InitOp.() {
            this.dir = originDirectory.absolutePathString()
        })
        disableGpgSign(originDirectory.absolutePathString())
        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        execute()

        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertEquals("1.0.0", gitAdapter.showTag("HEAD"))
    }
}
