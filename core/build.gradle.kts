plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  application
}

group = "me.user"
version = "1.0-SNAPSHOT"

val ktorVersion = "1.6.7"
val spaceUsername: String by project
val spacePassword: String by project

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-apache:$ktorVersion")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

application {
  mainClass.set("MainKt")
}
