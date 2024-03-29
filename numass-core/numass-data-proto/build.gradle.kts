import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
    id("com.google.protobuf") version "0.8.16"
}


repositories {
    mavenCentral()
}

dependencies {
    api("com.google.protobuf:protobuf-java:3.17.1")
    api(project(":numass-core:numass-data-api"))
    api(project(":dataforge-storage"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    dependsOn(":numass-core:numass-data-proto:generateProto")
}

//sourceSets {
//    create("proto") {
//        proto {
//            srcDir("src/main/proto")
//        }
//    }
//}

protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.17.1"
    }
    generatedFilesBaseDir = "$projectDir/gen"
}

//tasks.getByName("clean").doLast{
//    delete(protobuf.protobuf.generatedFilesBaseDir)
//}