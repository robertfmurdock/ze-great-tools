package com.zegreatrob.tools.tagger

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class GithubReleaseUploadArguments : CommandLineArgumentProvider {
    @get:Input
    abstract val version: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assets: ConfigurableFileCollection

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    override fun asArguments(): Iterable<String> {
        val directory = projectDirectory.get().asFile
        val paths = assets.files.map { it.relativeTo(directory).path }
        return listOf("-c", uploadScript, "sh", version.get()) + paths
    }

    private val uploadScript = """
        version="$1"
        shift
        for asset_file in "$@"; do
            asset_name=$(basename "${'$'}asset_file")
            if gh release view "${'$'}version" --json assets --jq ".assets[].name" | grep -q "^${'$'}asset_name$"; then
                echo "Asset ${'$'}asset_name already uploaded to release ${'$'}version, skipping"
            else
                gh release upload "${'$'}version" "${'$'}asset_file"
            fi
        done
    """.trimIndent()
}
