plugins {
    kotlin("jvm") version "1.5.31"
    id("org.openjfx.javafxplugin") version "0.0.10" apply false
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "inr.numass"
    version = "1.1.0"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        api(kotlin("reflect"))
        api("org.jetbrains:annotations:23.0.0")
        testImplementation("junit:junit:4.13.2")

        //Spock dependencies. To be removed later
        // https://mvnrepository.com/artifact/org.spockframework/spock-core
        testImplementation("org.spockframework:spock-core:2.0-groovy-3.0")

    }

    tasks {
        compileJava{
            options.encoding = "UTF-8"
        }

        compileTestJava{
            options.encoding = "UTF-8"
        }

        compileKotlin {
            kotlinOptions {
                jvmTarget = "16"
                javaParameters = true
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xjvm-default=all",
                    "-progressive",
                    "-Xuse-experimental=kotlin.Experimental"
                )
            }
        }

        compileTestKotlin {
            kotlinOptions {
                jvmTarget = "16"
                javaParameters = true
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xjvm-default=all",
                    "-progressive",
                    "-Xuse-experimental=kotlin.Experimental"
                )
            }
        }
    }
}