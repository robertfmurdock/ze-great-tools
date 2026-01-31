package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class CollectDepsTask : DefaultTask() {
    @get:Input
    abstract val localDeps: Property<String>
}
