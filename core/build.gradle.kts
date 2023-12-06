plugins {
  id("io.ktor.plugin") version "2.2.4"
  kotlin("plugin.serialization") version "1.9.21"
}

val ktorVersion = "2.2.4"

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-apache:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}
