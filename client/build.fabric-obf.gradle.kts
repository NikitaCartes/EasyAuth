// Obfuscated-Fabric twin of build.fabric.gradle.kts (26.x ships deobfuscated; <26.1 needs
// officialMojangMappings + mod-remapped deps). Same loom artifact as build.fabric.gradle.kts
// (no double-load), but declared as `fabric-loom` — that id string is what makes Gradle generate
// the mappings/modImplementation/remapJar Kotlin accessors this script needs (the server's
// build.fabric-obf.gradle.kts does exactly this against build.fabric-deobf.gradle.kts).
plugins {
    id("java")
    id("java-library")
    kotlin("jvm") version "2.4.0"
    id("fabric-loom") version "1.17-SNAPSHOT"
    id("com.google.devtools.ksp") version "2.3.9"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
    id("com.gradleup.shadow") version "9.4.2"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

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
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    // Mod-remapped so its references resolve against Mojang mappings on obf targets.
    // isTransitive=false: we only compile against the ModMenuApi/ConfigScreenFactory interfaces,
    // and older ModMenu builds pull mod deps (placeholder-api) not in these repos.
    modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}") { isTransitive = false }
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
    from(tasks.remapJar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}
