plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
}
stonecutter active "1.21.11"

// Expose `fabric` / `neoforge` boolean constants to `//? if` comments based on the node id suffix.
// Bare nodes (e.g. "1.21.11") have no "-" suffix and default to "fabric".
stonecutter parameters {
    constants.match(current.project.substringAfterLast('-', "fabric"), "fabric", "neoforge")
}

stonecutter.tasks {
    order("publishMods")
}