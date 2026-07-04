plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.141"
}

stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

apply(from = rootProject.file("gradle/mod-version.gradle.kts"))
val dynamicVersion = project.extra["dynamicVersion"] as String

base.archivesName = "${property("client_mod_id")}-neoforge-mc${property("minecraft_version")}"

repositories {
    mavenCentral()
    maven(url = "https://maven.neoforged.net/releases")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

neoForge {
    version = property("neoforge_version").toString()

    runs {
        create("client") {
            client()
            gameDirectory.set(file("run"))
        }
    }

    mods {
        create(property("client_mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.processResources {
    val expansions = mapOf(
        "version" to dynamicVersion,
        "supported_minecraft_version" to project.property("supported_minecraft_version"),
        "neoforge_version" to project.property("neoforge_version"),
        "client_mod_id" to project.property("client_mod_id"),
        "client_mod_name" to project.property("client_mod_name")
    )
    inputs.properties(expansions)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expansions)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.jar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}
