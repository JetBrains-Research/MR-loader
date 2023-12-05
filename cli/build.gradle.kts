import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val version = "0.0.1"

plugins {
    kotlin("jvm") version "1.9.21"
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.google.cloud.tools.jib") version "3.4.0"
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "cli.ShadowJarCLI"))
        }
    }

    jib {
        from {
            image = "maven:3.9.0-eclipse-temurin-17"

            platforms {
                platform {
                    architecture = if (System.getProperty("os.arch").equals("aarch64")) "arm64" else "amd64"
                    os = "linux"
                }
            }
        }
        to {
            image = "ghcr.io/jetbrains-research/mr-loader/${rootProject.name}:$version"
            tags = setOf("latest", version)
        }
    }

}

application {
    mainClass.set("cli.ShadowJarCLIKt")
}


dependencies {
    implementation(project(":core"))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC")
}

repositories {
    mavenCentral()
}
