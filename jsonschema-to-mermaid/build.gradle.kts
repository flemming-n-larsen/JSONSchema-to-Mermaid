plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
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
    mainClass.set("com.github.flemming_n_larsen.mermaid_class_diagram_generator.AppKt")
}

tasks.test {
    useJUnitPlatform()
}