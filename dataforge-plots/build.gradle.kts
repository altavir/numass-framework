plugins {
    id("org.openjfx.javafxplugin")
}

javafx {
    modules = listOf("javafx.controls")
    version = "11"
}

description = "dataforge-plots"

dependencies {
    api(project(":dataforge-core"))
}
