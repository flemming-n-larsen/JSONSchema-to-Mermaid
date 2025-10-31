val artifactBaseName = "jsonschema-to-mermaid"

plugins {
    application
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.2.2"

    id("com.github.ben-manes.versions") version "0.53.0" // dependencyUpdates task
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Command-line (CLI) support
    implementation("com.github.ajalt.clikt", "clikt", "5.0.3")

    // JSONSchema
    implementation("net.pwall.json", "json-kotlin-schema", "0.57")

    // Gson for JSON parsing
    implementation("com.google.code.gson", "gson", "2.13.2")

    // SnakeYAML for YAML parsing
    implementation("org.yaml", "snakeyaml", "2.5")

    // Testing

    // Kotlin test (required)
    testImplementation(kotlin("test"))

    // Kotest (Kotlin testing)
    testImplementation("io.kotest", "kotest-runner-junit5", "6.0.3")
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

    register("generateVersionProperties") {
        val outputDir = file("src/main/resources")
        val outputFile = file("src/main/resources/version.properties")
        doLast {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val version = project.version.toString()
            outputFile.writeText("version=$version\n")
        }
    }

    named("processResources") {
        dependsOn("generateVersionProperties")
    }
}

kotlin {
    jvmToolchain(11)
}
