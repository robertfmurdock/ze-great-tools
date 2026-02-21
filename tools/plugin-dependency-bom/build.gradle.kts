plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    api(platform(libs.org.junit.junit.bom))
    api(platform(libs.org.jetbrains.kotlinx.kotlinx.coroutines.bom))
    constraints {
        api(libs.org.jetbrains.kotlinx.kotlinx.datetime)
    }
}
