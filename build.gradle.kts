import java.util.concurrent.TimeUnit

plugins {
    id("java")
    id("java-library")
    id("net.neoforged.moddev") version "2.0.141"
    id("com.gradleup.shadow") version "9.3.0"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

val baseVersion = property("mod_version").toString()
val dynamicVersion = if (baseVersion.endsWith("-SNAPSHOT")) {
    val lastReleaseTag = runGit("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*", "--abbrev=0")
    if (lastReleaseTag != null) {
        val countStr = runGit("rev-list", "$lastReleaseTag..HEAD", "--count")
        val count = countStr?.toIntOrNull() ?: 0
        if (count > 0) "$baseVersion.$count" else baseVersion
    } else baseVersion
} else baseVersion
version = dynamicVersion

base.archivesName = "${property("mod_id")}-neoforge-mc${property("minecraft_version")}"

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

neoForge {
    version = property("neoforge_version").toString()

    accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")

    runs {
        create("server") {
            server()
            gameDirectory.set(file("run"))
        }
    }

    mods {
        create("easyauth") {
            sourceSet(sourceSets.main.get())
        }
    }
}

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

    implementAndJarJar("de.mkammerer:argon2-jvm:${property("argon2_version")}", property("argon2_version").toString())
    implementAndJarJar("de.mkammerer:argon2-jvm-nolibs:${property("argon2_version")}", property("argon2_version").toString())
    implementAndJarJar("net.java.dev.jna:jna:${property("jna_version")}", property("jna_version").toString())
    implementAndJarJar("at.favre.lib:bcrypt:${property("bcrypt_version")}", property("bcrypt_version").toString())
    implementAndJarJar("at.favre.lib:bytes:${property("bytes_version")}", property("bytes_version").toString())
    implementAndJarJar("org.iq80.leveldb:leveldb:${property("leveldb_version")}", property("leveldb_version").toString())
    implementAndJarJar("org.iq80.leveldb:leveldb-api:${property("leveldb_version")}", property("leveldb_version").toString())
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
    dependsOn(tasks.shadowJar)
    from(zipTree(tasks.shadowJar.get().archiveFile)) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    val mainClassesDirs = sourceSets.main.get().output.classesDirs.files
    exclude { element ->
        element.file.extension == "class" && mainClassesDirs.any { element.file.toPath().startsWith(it.toPath()) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    val expansions = mapOf(
        "version" to dynamicVersion,
        "supported_minecraft_version" to project.property("supported_minecraft_version"),
        "minecraft_version" to project.property("minecraft_version"),
        "neoforge_version" to project.property("neoforge_version"),
        "java_version" to 25,
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name")
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

tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.jar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""

    file = tasks.jar.get().archiveFile
    dryRun = modrinthToken.isEmpty() || curseforgeToken.isEmpty()

    displayName = "${property("display_name")} $dynamicVersion"
    version = dynamicVersion

    changelog = file("RELEASE_NOTE.md").readText()
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
}

fun runGit(vararg args: String): String? = try {
    val proc = ProcessBuilder("git", *args).redirectErrorStream(true).start()
    proc.waitFor(5, TimeUnit.SECONDS)
    if (proc.exitValue() == 0) proc.inputStream.bufferedReader().readText().trim().takeIf { it.isNotBlank() } else null
} catch (_: Exception) { null }
