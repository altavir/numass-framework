plugins {
    id("org.openjfx.javafxplugin")
}

javafx {
    modules = listOf("javafx.controls")
    version = "11"
}

description = "jFreeChart plugin"

dependencies {
    api("org.jfree:jfreesvg:3.4.2")
    // https://mvnrepository.com/artifact/org.jfree/org.jfree.chart.fx
    api(group= "org.jfree", name= "jfreechart-fx", version= "1.0.1")


    api(project(":dataforge-plots"))
}