plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.fatsan"
version = "1.0.0"
description = "Villager trade balancing plugin for Folia and Paper 1.21.x"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.release = 21
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        val props = mapOf(
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
