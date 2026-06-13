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
        // Every node is "<mc>-<loader>"; the second element of each pair is the logical
        // version used by `//? if >=...` checks (see stonecutter.properties.toml sections).

        // Obfuscated Fabric (build.fabric-obf.gradle.kts: fabric-loom + officialMojangMappings).
        listOf("1.19.4", "1.20", "1.20.2", "1.20.3", "1.20.5", "1.21", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11").forEach { mc ->
            versions("$mc-fabric" to mc).buildscript("build.fabric-obf.gradle.kts")
        }
        // Deobfuscated Fabric (26.1+ ships unobfuscated): no remapping, no mappings.
        versions("26.1-fabric" to "26.1").buildscript("build.fabric-deobf.gradle.kts")
        // NeoForge (build.neoforge.gradle.kts, ModDevGradle). MDG 2.x only consumes NeoForge
        // 21.0.x+ (plus backported 20.4/20.6), so MC 1.20.2/1.20.3/1.20.5 have no NeoForge node.
        listOf("1.21", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1").forEach { mc ->
            versions("$mc-neoforge" to mc).buildscript("build.neoforge.gradle.kts")
        }
        vcsVersion = "1.21.11-fabric"
    }
}