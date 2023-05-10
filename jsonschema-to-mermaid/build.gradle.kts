val artifactBaseName = "jsonschema-to-mermaid"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
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
}

application {
    mainClass.set("jsonschema_to_mermaid.AppKt")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = application.mainClass
        }
    }

    shadowJar.configure {
        dependsOn(jar)
        archiveBaseName.set(artifactBaseName)
        archiveClassifier.set(null as String?) // get rid of "-all" classifier
    }

    test {
        useJUnitPlatform()
    }
}