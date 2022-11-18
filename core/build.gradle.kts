import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import java.io.FileOutputStream
import java.util.*


plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.expediagroup.graphql") version "5.3.2"
  application
}

group = "me.user"
version = "1.0-SNAPSHOT"

val ktorVersion = "1.6.7"
val spaceUsername: String by project
val spacePassword: String by project
val githubToken: String by project

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-apache:$ktorVersion")

  implementation("com.expediagroup:graphql-kotlin-ktor-client:5.3.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
  implementation("org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r")

  testImplementation(kotlin("test"))
}

graphql {
  client {
    endpoint = "https://api.github.com/graphql"
    packageName = "com.example.generated"
    headers = mapOf("Authorization" to "bearer $githubToken")
    serializer = GraphQLSerializer.KOTLINX
  }
}

tasks.test {
  useJUnitPlatform()
}


application {
  mainClass.set("MainKt")
}


val generatedPropertiesDir = "$buildDir/generated-properties"

sourceSets {
  main {
    kotlin {
      output.dir(generatedPropertiesDir)
    }
  }
}

tasks.register("generateProperties") {
  doLast {
    val propertiesFile = file("$generatedPropertiesDir/generated.properties")
    propertiesFile.parentFile.mkdirs()
    val properties = Properties()
    properties.setProperty("githubToken", githubToken)
    val out = FileOutputStream(propertiesFile)
    properties.store(out, null)
  }
}

tasks.named("run") {
  dependsOn("generateProperties")
}
