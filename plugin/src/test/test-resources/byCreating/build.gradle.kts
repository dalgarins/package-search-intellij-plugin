val kotlinVersion = "1.7.20-Beta"
val serializationVersion = "1.3.3"
val ktorVersion = "2.0.3"

plugins {
    kotlin("multiplatform") version "1.7.20-Beta"
    application //to run JVM part
    kotlin("plugin.serialization") version "1.7.20-Beta"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val THIS_DID_NOT_EXIST by creating {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
    }
}