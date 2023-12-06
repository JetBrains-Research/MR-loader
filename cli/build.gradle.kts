plugins {
    id("application")
    id("com.google.cloud.tools.jib") version "3.4.0"
}

val projectVersion = version as String

tasks {
    jib {
        container {
            mainClass = "cli.DockerCLIKt"
        }
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
            image = "ghcr.io/jetbrains-research/mr-loader/${rootProject.name}:$projectVersion"
            tags = setOf("latest", projectVersion)
        }
    }

}

application {
    mainClass.set("cli.DockerCLIKt")
}


dependencies {
    implementation(project(":core"))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC")
}

repositories {
    mavenCentral()
}
