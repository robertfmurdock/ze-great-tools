package com.zegreatrob.tools.tagger

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider

abstract class GithubReleaseCreateArguments : CommandLineArgumentProvider {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val draft: Property<Boolean>

    override fun asArguments(): Iterable<String> {
        val draftFlag = if (draft.get()) "--draft " else ""
        val script = """
            version="$1"
            if gh release view "${'$'}version" >/dev/null 2>&1; then
                echo "Release ${'$'}version already exists, skipping creation"
            else
                gh release create "${'$'}version" $draftFlag--title "${'$'}version" --notes "${'$'}version"
            fi
        """.trimIndent()
        return listOf("-c", script, "sh", version.get())
    }
}
