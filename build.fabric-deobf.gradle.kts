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
// (e.g. [fabric."26.1"]) resolve to bare property("...") names.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

apply(from = rootProject.file("gradle/mod-version.gradle.kts"))
val dynamicVersion = project.extra["dynamicVersion"] as String

repositories {
    maven(url = "https://maven.nucleoid.xyz")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://repo.opencollab.dev/main")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.terraformersmc.com/releases")
}

base.archivesName = "${property("mod_id")}-fabric-mc${property("minecraft_version")}"

val awFile = when {
    stonecutter.eval(stonecutter.current.version, ">=26.1") -> "easyauth.26.1.accesswidener"
    else -> throw GradleException("Access widener is missing for Minecraft ${stonecutter.current.version})")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

loom {
    splitEnvironmentSourceSets()
    accessWidenerPath = rootProject.file("src/main/resources/accesswidener/$awFile")
    mods {
        create("easyauth") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "run"
    }
}

fabricApi.configureTests {
    createSourceSet = true
    modId = "${property("mod_id")}-mixin-test"
    eula = true
    enableClientGameTests = false
    enableGameTests = true
}

tasks.named("runGameTest") {
    usesService(semaphore)
}

dependencies {
    fun implementAndInclude(name: String) {
        implementation(name)
        include(name)
    }

    fun implementAndShadow(name: String) {
        implementation(name)
        shadow(name)
    }

    // Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Translations
    include("xyz.nucleoid:server-translations-api:${property("server_translations_version")}")
    implementation("xyz.nucleoid:server-translations-api:${property("server_translations_version")}")

    // Permissions
    implementation("me.lucko:fabric-permissions-api:${property("fabric_permissions_version")}")
    compileOnly("net.luckperms:api:${property("luckperms_version")}")

    // Mods
    compileOnly("org.geysermc.floodgate:api:${property("floodgate_api_version")}")
    compileOnly("maven.modrinth:vanish:${property("vanish_version")}")

    // ModMenu config-screen entrypoint (client source set only; compile-only, loaded only when ModMenu is present)
    "clientCompileOnly"("com.terraformersmc:modmenu:${property("modmenu_version")}")

    // Password hashing
    implementAndInclude("at.favre.lib:bcrypt:${property("bcrypt_version")}")
    implementAndInclude("at.favre.lib:bytes:${property("bytes_version")}")

    // Storage
    implementAndShadow("org.mongodb:mongodb-driver-sync:${property("mongodb_version")}")
    implementAndShadow("org.mongodb:mongodb-driver-core:${property("mongodb_version")}")
    implementAndShadow("org.mongodb:bson:${property("mongodb_version")}")

    implementAndInclude("com.mysql:mysql-connector-j:${property("mysql_version")}")
    implementAndInclude("org.xerial:sqlite-jdbc:${property("sqlite_version")}")

    implementAndInclude("org.postgresql:postgresql:${property("postgresql_version")}")

    implementation("org.spongepowered:configurate-hocon:${property("hocon_version")}")
    shadow("org.spongepowered:configurate-hocon:${property("hocon_version")}")
}

tasks.shadowJar {
    archiveClassifier.set("dev-shadow")
    relocate("org.spongepowered.configurate", "xyz.nikitacartes.shadow.configurate")
    relocate("com.typesafe.config", "xyz.nikitacartes.shadow.config")
    relocate("io.leangen.geantyref", "xyz.nikitacartes.shadow.geantyref")
    relocate("net.kyori.option", "xyz.nikitacartes.shadow.option")
    relocate("org.bson", "xyz.nikitacartes.shadow.bson")
    relocate("com.mongodb", "xyz.nikitacartes.shadow.mongodb")

    configurations = listOf(project.configurations.shadow.get())
    from(sourceSets.main.get().output)
    from(sourceSets["client"].output)
}

tasks.jar {
    from("LICENCE")
    dependsOn(tasks.shadowJar)
    from(zipTree(tasks.shadowJar.get().archiveFile)) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    val sourceSetDirs = sourceSets.flatMap { it.output.files }.map { it.toPath() }
    exclude { element -> sourceSetDirs.any { element.file.toPath().startsWith(it) } }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to dynamicVersion,
                "supported_minecraft_version" to project.property("supported_minecraft_version"),
                "accessWidener" to awFile
            )
        )
    }

    // Deobfuscated MC needs no mixin refmap.
    filesMatching("easyauth.mixins.json") {
        filter {
            it.replace($$"\"refmap\": \"${refmap}\",", "")
        }
    }
}

tasks.processTestResources {
    dependsOn("kspGametestKotlin")
}

tasks.named<Copy>("processGametestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("kspTestKotlin")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.jar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}

java {
    withSourcesJar()
}

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""
    val githubToken = System.getenv("GITHUB_TOKEN") ?: ""

    file = tasks.jar.get().archiveFile
    dryRun = modrinthToken.isEmpty() || curseforgeToken.isEmpty() || githubToken.isEmpty()

    displayName = "${property("display_name")} $dynamicVersion"
    version = dynamicVersion

    changelog = file("../../RELEASE_NOTE.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    val targets = property("supported_versions").toString().split(",")

    modrinth {
        projectId = "aZj58GfX"
        accessToken = modrinthToken
        targets.forEach(minecraftVersions::add)
        requires("fabric-api")
        optional("luckperms")
        optional("vanish")
    }

    curseforge {
        projectId = "503866"
        accessToken = curseforgeToken
        targets.forEach(minecraftVersions::add)
        requires("fabric-api")
        embeds("server-translation-api")
        optional("luckperms")
        optional("meliusvanish")
    }
    // Uploads this node's jar into the single release created by the root publishGithub task.
    github {
        accessToken = githubToken
        parent(rootProject.tasks.named("publishGithub"))
    }
}

fletchingTable {
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

private abstract class ServerRunSemaphore : BuildService<BuildServiceParameters.None>

private val semaphore = gradle.sharedServices.registerIfAbsent("semaphore", ServerRunSemaphore::class.java) {
    maxParallelUsages.set(1)
}
