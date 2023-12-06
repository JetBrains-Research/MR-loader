plugins {
  kotlin("jvm") version "1.9.21"
}

repositories {
  mavenCentral()
}

subprojects {
  group = "org.jetbrains.research.ictl"
  version = "0.0.2"

  apply {
    apply(plugin = "org.jetbrains.kotlin.jvm")
  }
}