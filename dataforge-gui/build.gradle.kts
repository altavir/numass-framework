plugins {
    id("org.openjfx.javafxplugin")
}

javafx {
    modules = listOf("javafx.controls", "javafx.web")
    version = "11"
}

description = "A tornadofx based kotlin library"

dependencies {
    api(project(":dataforge-plots"))
    api(project(":dataforge-gui:dataforge-html"))
    api("no.tornado:tornadofx:1.7.20"){
        exclude("org.jetbrains.kotlin")
    }
    api("org.controlsfx:controlsfx:11.1.0")
    api("no.tornado:tornadofx-controlsfx:0.1.1")
    api("org.fxmisc.richtext:richtextfx:0.10.7")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.5.2")

    // optional dependency for JFreeChart
    //compileOnly project(":dataforge-plots:plots-jfc")
}


