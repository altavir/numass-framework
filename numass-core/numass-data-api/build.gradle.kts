import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
}


repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("hep.dataforge:dataforge-core")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}