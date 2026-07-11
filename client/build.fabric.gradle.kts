// The plugin set must exactly match build.fabric-deobf.gradle.kts: Gradle shares the
// buildscript classloader only for identical plugin classpaths, and Loom's shared build
// services (JarManifestService) break if Loom is loaded twice. Unused plugins here
// (kotlin/ksp/fletching-table/shadow) exist only for that parity.
plugins {
    id("java")
    id("java-library")
    kotlin("jvm") version "2.4.0"
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("com.google.devtools.ksp") version "2.3.9"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
    id("com.gradleup.shadow") version "9.4.2"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

// Tag this node's loader and version so the per-node values in stonecutter.properties.toml
// (e.g. [fabric."26.2"]) resolve to bare property("...") names.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

apply(from = rootProject.file("gradle/mod-version.gradle.kts"))
val dynamicVersion = project.extra["dynamicVersion"] as String

base.archivesName = "${property("client_mod_id")}-fabric-mc${property("minecraft_version")}"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// Wire-format/TOTP sources shared with the server mod (single source of truth,
// see xyz.nikitacartes.easyauth.protocol.ClientModProtocol). Version- and loader-independent,
// no stonecutter markers.
sourceSets["main"].java.srcDir(rootProject.projectDir.resolve("shared/src/main/java"))

loom {
    mods {
        create(property("client_mod_id").toString()) {
            sourceSet(sourceSets["main"])
        }
    }

    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "run"
    }
}

repositories {
    maven(url = "https://maven.terraformersmc.com/releases")
}

dependencies {
    // 26.x ships deobfuscated, so no mappings dependency (same as build.fabric-deobf.gradle.kts)
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    // Compile-only: the modmenu entrypoint class is loaded only when ModMenu is installed
    compileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to dynamicVersion,
                "supported_minecraft_version" to project.property("supported_minecraft_version")
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

// Shadow is applied only for plugin-classpath parity (see plugins block); nothing to shade.
tasks.shadowJar {
    enabled = false
}

tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.jar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}
