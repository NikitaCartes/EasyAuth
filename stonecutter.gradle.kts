plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
}
stonecutter active "1.21.11-fabric"

// Expose `fabric` / `neoforge` boolean constants to `//? if` comments based on the node's loader suffix.
stonecutter parameters {
    constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge")

    // Mojmap renamed ResourceLocation -> Identifier in 1.21.11. Files opt in with a
    // `//~ resource_location` header, so this one rule replaces the per-file
    // `//? if >= 1.21.11` import/usage toggles. Scoped (not global) because unrelated
    // identifiers like `customPacketIdentifier` contain the substring "Identifier".
    replacements.string(current.parsed >= "1.21.11", "resource_location") {
        replace("ResourceLocation", "Identifier")
        replace("location()", "identifier()")
    }
}

stonecutter.tasks {
    order("publishMods")
}