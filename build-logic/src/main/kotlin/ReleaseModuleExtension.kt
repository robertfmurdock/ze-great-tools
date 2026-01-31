import org.gradle.api.provider.Property

abstract class ReleaseModuleExtension {
    abstract val includeInRelease: Property<Boolean>
}
