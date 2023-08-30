@file:Suppress("UnstableApiUsage")

plugins {
    id(packageSearchCatalog.plugins.kotlin.jvm)
    id(packageSearchCatalog.plugins.dokka)
    id(packageSearchCatalog.plugins.idea.gradle.plugin)
    alias(packageSearchCatalog.plugins.kotlin.plugin.serialization)
    `build-config`
    `maven-publish`
}

packagesearch {
    publication {
        isEnabled = true
        artifactId = "packagesearch-plugin-core"
    }
}

dependencies {
    api(packageSearchCatalog.packagesearch.api.client)
    api(packageSearchCatalog.nitrite) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "org.slf4j")
    }
    testRuntimeOnly(packageSearchCatalog.junit.jupiter.api)
    testRuntimeOnly(packageSearchCatalog.junit.jupiter.engine)
    testImplementation(packageSearchCatalog.kotlinx.coroutines.test)
    testImplementation(kotlin("test-junit5"))
}

tasks {
    withType<Test> {
        environment("DB_PATH", layout.buildDirectory.file("tests/cache.db").get().asFile.absolutePath)
    }
}