plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
}
stonecutter active "1.21.11-fabric"

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