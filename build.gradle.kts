plugins {
    id("dev.kikugie.j52j") version "2.0"
    id("java")
    id("java-library")
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("com.gradleup.shadow") version "9.0.0-beta13"
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
    else -> "easyauth.1.20.accesswidener"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

loom {
    accessWidenerPath = file("../../src/main/resources/accesswidener/$awFile")
    serverOnlyMinecraftJar()
    log4jConfigs.from(file("log4j.xml"))

    runConfigs.all {
        ideConfigGenerated(true) // Run configurations are not created for subprojects by default
        runDir = "../../run" // Use a shared run folder and create separate worlds
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

    filesMatching("easyauth.mixins.json5") {
        filter {
            it.replace("\${refmap}", "${base.archivesName.get()}-refmap.json")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

java {
    withSourcesJar()
}

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""

    file = project.tasks.remapJar.get().archiveFile
    dryRun = modrinthToken.isEmpty() || curseforgeToken.isEmpty()

    displayName = "${property("display_name")} ${property("version")}"
    version = "${property("version")}"

    changelog = "Release notes:\n${file("../../RELEASE_NOTE.md").readText()}\n\nFull Changelog:\nhttps://github.com/NikitaCartes/EasyAuth/tree/HEAD/CHANGELOG.md"
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

j52j {
    params {
        prettyPrinting = true
    }
}