plugins {
    idea
    kotlin("jvm")
}


repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(project(":numass-core:numass-data-api"))

    // https://mvnrepository.com/artifact/org.apache.commons/commons-collections4
    compile(group = "org.apache.commons", name = "commons-collections4", version = "4.3")

}