plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.1.1"
}
stonecutter active "26.2-fabric"

stonecutter parameters {
    constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge")

    replacements.string(current.parsed >= "1.21.11", "resource_location") {
        replace("ResourceLocation", "Identifier")
        replace("location()", "identifier()")
    }
}

stonecutter.tasks {
    order("publishMods")
}

// One GitHub release for the whole version matrix: this root task creates it (empty),
// and every node's publishGithub uploads its jar into it via `parent`.
// The toml's top-level mod_version may not be injected into the root project, so feed it
// to mod-version.gradle.kts (same dynamic version as the nodes) manually if missing.
if (findProperty("mod_version") == null) {
    extra["mod_version"] = file("stonecutter.properties.toml").readLines()
        .first { it.trim().startsWith("mod_version") }
        .substringAfter('=').trim().trim('"')
}
apply(from = file("gradle/mod-version.gradle.kts"))
val dynamicVersion = extra["dynamicVersion"] as String

publishMods {
    val githubToken = System.getenv("GITHUB_TOKEN") ?: ""

    dryRun = githubToken.isEmpty()
    version = dynamicVersion
    displayName = dynamicVersion
    changelog = rootProject.file("RELEASE_NOTE.md").readText()
    type = STABLE

    github {
        accessToken = githubToken
        repository = "NikitaCartes/EasyAuth"
        commitish = "stonecutter"
        tagName = dynamicVersion
        allowEmptyFiles = true
    }
}