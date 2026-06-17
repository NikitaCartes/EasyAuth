import java.util.concurrent.TimeUnit

// Shared mod-version computation for every loader build script.
// Applied with `apply(from = rootProject.file("gradle/mod-version.gradle.kts"))`.
// Sets `project.version` and exposes the result as `extra["dynamicVersion"]`.

fun runGit(vararg args: String): String? = try {
    val proc = ProcessBuilder("git", *args).redirectErrorStream(true).start()
    proc.waitFor(5, TimeUnit.SECONDS)
    if (proc.exitValue() == 0) proc.inputStream.bufferedReader().readText().trim().takeIf { it.isNotBlank() } else null
} catch (_: Exception) { null }

val baseVersion = property("mod_version").toString()
val dynamicVersion = if (baseVersion.endsWith("-SNAPSHOT")) {
    // Match only plain release tags like 1.2.3
    val lastReleaseTag = runGit("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*", "--abbrev=0")
    if (lastReleaseTag != null) {
        // Count commits since last release tag
        val countStr = runGit("rev-list", "$lastReleaseTag..HEAD", "--count")
        val count = countStr?.toIntOrNull() ?: 0
        if (count > 0) "$baseVersion.$count" else baseVersion
    } else baseVersion
} else baseVersion

version = dynamicVersion
extra["dynamicVersion"] = dynamicVersion
