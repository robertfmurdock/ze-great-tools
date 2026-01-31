// ---- Plugin wiring -------------------------------------------------------

if (project == rootProject) {

    subprojects {
        extensions.create<ReleaseModuleExtension>("releaseModule").apply {
            includeInRelease.convention(false)
        }
    }

    tasks.register<WriteReleaseFingerprint>("writeReleaseFingerprint") {
        outputFile.set(
            layout.buildDirectory.file("release-fingerprint.txt")
        )
    }
}
