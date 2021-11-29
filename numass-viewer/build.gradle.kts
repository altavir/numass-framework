plugins {
    kotlin("jvm")
    id("org.openjfx.javafxplugin")
    //id("com.github.johnrengelman.shadow")
    id("org.beryx.runtime") version "1.12.7"
    application
}

javafx {
    modules = listOf("javafx.graphics", "javafx.controls", "javafx.web")
    version = "11"
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("inr.numass.viewer.Viewer")
}

version = "0.6.0"

description = "The viewer for numass data"

dependencies {
    api(project(":numass-core"))
    api(project(":dataforge-plots:plots-jfc"))
    api(project(":dataforge-gui"))
}

val addJvmArgs = listOf(
    "-XX:+UseZGC",
    "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
    "--add-opens=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED",
    "--add-opens=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED",
    "--add-opens=javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED",
    "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED",
    "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED",
    "--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED",
    "--add-opens=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED",
    "--add-exports=javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED",
)

application {
    applicationDefaultJvmArgs = addJvmArgs
}

runtime {
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    addModules(
        "java.desktop",
        "jdk.unsupported",
        "java.scripting",
        "java.logging",
        "java.xml",
        "javafx.graphics",
        "javafx.controls"
    )
    jpackage {
        //installerType = "deb"
        jvmArgs = addJvmArgs
        val currentOs = org.gradle.internal.os.OperatingSystem.current()
        installerOptions = installerOptions + listOf("--vendor", "MIPT-NPM lab")

        if (currentOs.isWindows) {
            installerOptions = installerOptions + listOf(
                "--win-menu",
                "--win-menu-group", "Numass",
                "--win-dir-chooser",
                "--win-shortcut"
            )
        } else if (currentOs.isLinux) {
            installerType = "deb"
            installerOptions = installerOptions + listOf(
                "--linux-package-name", "numass-viewer",
                "--linux-shortcut",
                "--linux-deb-maintainer", "nozik.aa@mipt.ru",
                "--linux-menu-group", "Science",
                "--linux-shortcut"
            )
        }
    }
    launcher {
        jvmArgs = addJvmArgs
    }
}