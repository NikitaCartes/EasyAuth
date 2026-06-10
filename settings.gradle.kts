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
        // Obfuscated Fabric nodes (default build.gradle.kts: fabric-loom + officialMojangMappings).
        versions("1.19.4", "1.20", "1.20.2", "1.20.3", "1.20.5", "1.21", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11")
        // Deobfuscated Fabric (26.1+ ships unobfuscated): net.fabricmc.fabric-loom, no remapping, no mappings.
        versions("26.1" to "26.1").buildscript("build.fabric-deobf.gradle.kts")
        // NeoForge nodes (build.neoforge.gradle.kts, ModDevGradle). The "<mc>-neoforge" to "<mc>"
        // mapping keeps the logical version correct for `//? if >=...` checks.
        // NB: ModDevGradle 2.x can only consume NeoForge 21.0.x+ (plus the backported 20.4/20.6 lines).
        // MC 1.20.2/1.20.3/1.20.5 (NeoForge 20.2/20.3/20.5) never published the `neoforge-moddev-bundle`
        // variant MDG requires, so they cannot be built here (Fabric still covers those MC versions).
        listOf("1.21", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1").forEach { mc ->
            versions("$mc-neoforge" to mc).buildscript("build.neoforge.gradle.kts")
        }
        vcsVersion = "1.21.11"
    }
}