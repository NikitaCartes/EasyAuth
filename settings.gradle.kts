pluginManagement {
    repositories {
        maven ("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.2"
}

stonecutter {
    create(rootProject) {

        // Obfuscated Fabric
        listOf("1.19.4", "1.20", "1.20.2", "1.20.3", "1.20.5", "1.21", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11").forEach { mc ->
            versions("$mc-fabric" to mc).buildscript("build.fabric-obf.gradle.kts")
        }
        // Deobfuscated Fabric
        versions("26.1-fabric" to "26.1").buildscript("build.fabric-deobf.gradle.kts")
        // NeoForge
        listOf("1.21", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1").forEach { mc ->
            versions("$mc-neoforge" to mc).buildscript("build.neoforge.gradle.kts")
        }
        vcsVersion = "1.21.11-fabric"
    }
}