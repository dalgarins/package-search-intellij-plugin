plugins {
    id(packageSearchCatalog.plugins.kotlin.jvm)
    id(packageSearchCatalog.plugins.idea.gradle.plugin)
    id(packageSearchCatalog.plugins.dokka)
    alias(packageSearchCatalog.plugins.compose.desktop)
    alias(packageSearchCatalog.plugins.kotlin.plugin.serialization)
    `build-config`
    `maven-publish`
}

packagesearch {
    publication {
        isEnabled.set(true)
        artifactId.set("packagesearch-plugin")
    }
}

dependencies {
    implementation(packageSearchCatalog.jewel.foundation)
    implementation(packageSearchCatalog.packagesearch.api.models)
    compileOnly(packageSearchCatalog.kotlinx.serialization.core)
    implementation(projects.plugin.maven)
    implementation(projects.plugin.gradle.base)
    implementation(projects.plugin.core)
    implementation(projects.plugin.gradle.kmp)
    testImplementation(kotlin("test-junit5"))
    testImplementation(packageSearchCatalog.junit.jupiter.api)
    testRuntimeOnly(packageSearchCatalog.junit.jupiter.engine)
}

tasks {
    publishPlugin {
        toolboxEnterprise.set(true)
        host.set("https://tbe.labs.jb.gg/")
        token.set(
            project.properties["toolboxEnterpriseToken"]?.toString()
                ?: System.getenv("TOOLBOX_ENTERPRISE_TOKEN")
        )
        channels.set(listOf("INTERNAL-EAP"))
    }
}