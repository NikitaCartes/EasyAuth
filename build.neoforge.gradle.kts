plugins {
    id("java")
    id("java-library")
    kotlin("jvm") version "2.4.0"
    id("com.google.devtools.ksp") version "2.3.9"
    id("net.neoforged.moddev") version "2.0.141"
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.22"
    id("com.gradleup.shadow") version "9.4.2"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

apply(from = rootProject.file("gradle/mod-version.gradle.kts"))
val dynamicVersion = project.extra["dynamicVersion"] as String

base.archivesName = "${property("mod_id")}-neoforge-mc${property("minecraft_version")}"

val atFile = when {
    stonecutter.eval(stonecutter.current.version, ">=1.21.11") -> "easyauth.1.21.11.cfg"
    stonecutter.eval(stonecutter.current.version, ">=1.21.9") -> "easyauth.1.21.9.cfg"
    stonecutter.eval(stonecutter.current.version, ">=1.21.6") -> "easyauth.1.21.6.cfg"
    stonecutter.eval(stonecutter.current.version, ">=1.21.5") -> "easyauth.1.21.5.cfg"
    stonecutter.eval(stonecutter.current.version, ">=1.20.3") -> "easyauth.1.20.3.cfg"
    stonecutter.eval(stonecutter.current.version, ">=1.20.2") -> "easyauth.1.20.2.cfg"
    else -> throw GradleException("Access transformer is missing for Minecraft ${stonecutter.current.version})")
}

val atSource = rootProject.file("src/main/resources/accesstransformer/$atFile")

repositories {
    mavenCentral()
    maven(url = "https://maven.neoforged.net/releases")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://api.modrinth.com/maven")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

// Client-only sources share the src/client layout with Fabric's split source sets. A "client"
// source set makes Stonecutter preprocess src/client here too (it maps the source set to
// src/client by name and wires the processed copy in — no manual srcDir needed). The client @Mod
// is dist-gated (Dist.CLIENT), so these classes never load on a dedicated server.
val clientSourceSet = sourceSets.create("client")
clientSourceSet.compileClasspath += sourceSets["main"].compileClasspath + sourceSets["main"].output
clientSourceSet.runtimeClasspath += sourceSets["main"].runtimeClasspath + sourceSets["main"].output

neoForge {
    version = property("neoforge_version").toString()

    accessTransformers.from(atSource)

    runs {
        create("server") {
            server()
            gameDirectory.set(file("run"))
        }
        create("client") {
            client()
            gameDirectory.set(file("run"))
        }
    }

    mods {
        create(property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
            sourceSet(clientSourceSet)
        }
    }
}


fletchingTable {
    neoforge {
        applyMixinConfig = false
    }
    mixins.create("main") {
        // env("SERVER"): all discovered mixins go in the config's "server" block, so they apply
        // only on a dedicated server (not the integrated server / single-player).
        mixin("default", "easyauth.mixins.json") {
            env("SERVER")
        }
    }
    lang.create("main") {
        // Nested YAML lang files are flattened to dotted-key JSON at build time
        patterns.add("data/easyauth/lang/**")
    }
    lang.create("client") {
        // Client-only nested YAML lang files (src/client source set)
        patterns.add("assets/easyauth/lang/**")
    }
}

// Configuration that gets shaded (relocated) into the main jar.
val shaded: Configuration by configurations.creating
configurations {
    compileOnly { extendsFrom(shaded) }
    runtimeOnly { extendsFrom(shaded) }
}

fun DependencyHandlerScope.implementAndJarJar(notation: String, version: String) {
    implementation(notation)
    jarJar(notation) {
        version { strictly("[$version,)"); prefer(version) }
    }
}

dependencies {
    compileOnly("net.luckperms:api:${property("luckperms_version")}")
    implementAndJarJar("io.github.llamalad7:mixinextras-neoforge:0.5.4", "0.5.4")

    implementAndJarJar("at.favre.lib:bcrypt:${property("bcrypt_version")}", property("bcrypt_version").toString())
    implementAndJarJar("at.favre.lib:bytes:${property("bytes_version")}", property("bytes_version").toString())
    implementAndJarJar("com.mysql:mysql-connector-j:${property("mysql_version")}", property("mysql_version").toString())
    implementAndJarJar("org.xerial:sqlite-jdbc:${property("sqlite_version")}", property("sqlite_version").toString())
    implementAndJarJar("org.postgresql:postgresql:${property("postgresql_version")}", property("postgresql_version").toString())

    shaded("org.mongodb:mongodb-driver-sync:${property("mongodb_version")}")
    shaded("org.mongodb:mongodb-driver-core:${property("mongodb_version")}")
    shaded("org.mongodb:bson:${property("mongodb_version")}")
    shaded("org.spongepowered:configurate-hocon:${property("hocon_version")}")
}

tasks.shadowJar {
    archiveClassifier.set("dev-shadow")
    configurations = listOf(shaded)

    mergeServiceFiles()

    relocate("org.spongepowered.configurate", "xyz.nikitacartes.shadow.configurate")
    relocate("com.typesafe.config", "xyz.nikitacartes.shadow.config")
    relocate("io.leangen.geantyref", "xyz.nikitacartes.shadow.geantyref")
    relocate("net.kyori.option", "xyz.nikitacartes.shadow.option")
    relocate("org.bson", "xyz.nikitacartes.shadow.bson")
    relocate("com.mongodb", "xyz.nikitacartes.shadow.mongodb")

    from(sourceSets.main.get().output)
}

tasks.jar {
    from("LICENSE")
    from(clientSourceSet.output)
    dependsOn(tasks.shadowJar)
    from(zipTree(tasks.shadowJar.get().archiveFile)) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    val mainOutputDirs = sourceSets.main.get().output.files
    exclude { element ->
        mainOutputDirs.any { element.file.toPath().startsWith(it.toPath()) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    // Ship only the version-correct access transformer, at the canonical path neoforge.mods.toml references.
    exclude("accesstransformer/**", "accesswidener/**", "easyauth.mixins.json.license")
    from(atSource) {
        into("META-INF")
        rename { "accesstransformer.cfg" }
    }

    val expansions = mapOf(
        "version" to dynamicVersion,
        "supported_minecraft_version" to project.property("supported_minecraft_version"),
        "minecraft_version" to project.property("minecraft_version"),
        "neoforge_version" to project.property("neoforge_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name")
    )
    inputs.properties(expansions)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expansions)
    }

    // NeoForge runs on official Mojang names, so no refmap is needed
    filesMatching("easyauth.mixins.json") {
        filter { it.replace($$"\"refmap\": \"${refmap}\",", "") }
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

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""
    val githubToken = System.getenv("GITHUB_TOKEN") ?: ""

    file = tasks.jar.get().archiveFile
    dryRun = modrinthToken.isEmpty() || curseforgeToken.isEmpty() || githubToken.isEmpty()

    displayName = "${property("display_name")} $dynamicVersion"
    version = dynamicVersion

    changelog = rootProject.file("RELEASE_NOTE.md").readText()
    type = STABLE
    modLoaders.add("neoforge")

    val targets = property("supported_versions").toString().split(",")

    modrinth {
        projectId = "aZj58GfX"
        accessToken = modrinthToken
        targets.forEach(minecraftVersions::add)
        optional("luckperms")
    }

    curseforge {
        projectId = "503866"
        accessToken = curseforgeToken
        targets.forEach(minecraftVersions::add)
        optional("luckperms")
    }

    // Uploads this node's jar into the single release created by the root publishGithub task.
    github {
        accessToken = githubToken
        parent(rootProject.tasks.named("publishGithub"))
    }
}
