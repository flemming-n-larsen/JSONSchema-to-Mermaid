val artifactBaseName = "jsonschema-to-mermaid"

plugins {
    application
    kotlin("jvm") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    // Command-line (CLI) support
    implementation("com.github.ajalt.clikt", "clikt", "3.5.2")
    // JSON
    implementation("com.google.code.gson", "gson", "2.10.1")
    // YAML
    implementation("org.yaml", "snakeyaml", "2.0")

    // Test
    testImplementation(kotlin("test"))
    // Kotest
    testImplementation("io.kotest", "kotest-runner-junit5", "5.6.2")
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set("jsonschema_to_mermaid.AppKt")
}

tasks {
    // to make `gradle build` happy
    startScripts.configure {
        dependsOn(shadowJar)
    }

    shadowJar.configure {
        dependsOn(jar)
        archiveBaseName.set(artifactBaseName)
        archiveClassifier.set(null as String?) // get rid of "-all" classifier
    }

    jar {
        manifest {
            attributes["Main-Class"] = application.mainClass
        }
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(11)
}