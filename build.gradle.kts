plugins {
    id("java")
    id("java-library")
    kotlin("jvm") version "2.2.0"
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.15"
    id("com.gradleup.shadow") version "9.0.2"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

repositories {
    maven(url = "https://maven.nucleoid.xyz")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://repo.opencollab.dev/main")
    maven(url = "https://api.modrinth.com/maven")
    //mavenLocal()
}

base.archivesName = "${property("mod_id")}-mc${property("minecraft_version")}"
version = "${property("mod_version")}"

val awFile = when {
    stonecutter.eval(stonecutter.current.version, ">=1.21.6") -> "easyauth.1.21.6.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.20.3") -> "easyauth.1.20.3.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.20.2") -> "easyauth.1.20.2.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.19.4") -> "easyauth.1.19.4.accesswidener"
    else -> throw GradleException("Access widener is missing for Minecraft ${stonecutter.current.version})")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

loom {
    accessWidenerPath = file("../../src/main/resources/accesswidener/$awFile")
    serverOnlyMinecraftJar()
    log4jConfigs.from(file("log4j.xml"))

    runConfigs.all {
        ideConfigGenerated(true) // Run configurations are not created for subprojects by default
        runDir = "run" // Use a separate run directory for all configurations
    }
}

dependencies {
    fun implementAndInclude(name: String) {
        implementation(name)
        include(name)
    }

    // Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Translations
    include("xyz.nucleoid:server-translations-api:${property("server_translations_version")}")
    modImplementation("xyz.nucleoid:server-translations-api:${property("server_translations_version")}")

    // Permissions
    modImplementation("me.lucko:fabric-permissions-api:${property("fabric_permissions_version")}")
    compileOnly("net.luckperms:api:${property("luckperms_version")}")

    // Mods
    modCompileOnly("org.geysermc.floodgate:api:${property("floodgate_api_version")}")
    modCompileOnly("maven.modrinth:vanish:${property("vanish_version")}")

    // Password hashing
    implementAndInclude("de.mkammerer:argon2-jvm:${property("argon2_version")}")
    implementAndInclude("de.mkammerer:argon2-jvm-nolibs:${property("argon2_version")}")

    implementAndInclude("at.favre.lib:bcrypt:${property("bcrypt_version")}")
    implementAndInclude("at.favre.lib:bytes:${property("bytes_version")}")

    // Storage
    implementAndInclude("org.iq80.leveldb:leveldb:${property("leveldb_version")}")
    implementAndInclude("org.iq80.leveldb:leveldb-api:${property("leveldb_version")}")

    implementAndInclude("org.mongodb:mongodb-driver-sync:${property("mongodb_version")}")
    implementAndInclude("org.mongodb:mongodb-driver-core:${property("mongodb_version")}")
    implementAndInclude("org.mongodb:bson:${property("mongodb_version")}")

    implementAndInclude("com.mysql:mysql-connector-j:${property("mysql_version")}")
    implementAndInclude("org.xerial:sqlite-jdbc:${property("sqlite_version")}")

    implementAndInclude("org.postgresql:postgresql:${property("postgresql_version")}")

    implementation("org.spongepowered:configurate-hocon:${property("hocon_version")}")
    shadow("org.spongepowered:configurate-hocon:${property("hocon_version")}")

    include("net.java.dev.jna:jna:${property("jna_version")}")
}

tasks.shadowJar {
    relocate("org.spongepowered.configurate", "xyz.nikitacartes.shadow.configurate")
    relocate("com.typesafe.config", "xyz.nikitacartes.shadow.config")
    relocate("io.leangen.geantyref", "xyz.nikitacartes.shadow.geantyref")
    relocate("net.kyori.option", "xyz.nikitacartes.shadow.option")

    minimize()
    configurations = listOf(project.configurations.shadow.get())
    from(sourceSets.main.get().output)
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    from("LICENCE")
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.property("mod_version"),
                "supported_minecraft_version" to project.property("supported_minecraft_version"),
                "accessWidener" to awFile
            )
        )
    }

    filesMatching("easyauth.mixins.json") {
        filter {
            it.replace("\${refmap}", "${base.archivesName.get()}-refmap.json")
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
    options.release.set(17)
}

java {
    withSourcesJar()
}

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""

    file = tasks.remapJar.get().archiveFile
    dryRun = modrinthToken.isEmpty() || curseforgeToken.isEmpty()

    displayName = "${property("display_name")} ${property("version")}"
    version = "${property("version")}"

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
}

fletchingTable {
    mixins.create("main") { // Name should match an existing source set
        // Default matches the default value in the annotation
        mixin("default", "easyauth.mixins.json")
    }
}

private abstract class ServerRunSemaphore : BuildService<BuildServiceParameters.None>

private val semaphore = gradle.sharedServices.registerIfAbsent("semaphore", ServerRunSemaphore::class.java) {
    maxParallelUsages.set(1)
}