plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.1.1" apply false
}
stonecutter active "1.21.11"

stonecutter.tasks {
    order("publishMods")
}