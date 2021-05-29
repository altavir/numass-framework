plugins {
    idea
    kotlin("jvm")
}


repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(project(":dataforge-maths"))
    api(project(":numass-core:numass-data-api"))

    // https://mvnrepository.com/artifact/org.apache.commons/commons-collections4
    implementation(group = "org.apache.commons", name = "commons-collections4", version = "4.3")

}