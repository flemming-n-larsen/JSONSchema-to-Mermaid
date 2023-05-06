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
    // JSON and YAML
    implementation("com.google.code.gson","gson", "2.10.1")

    // Test
    testImplementation(kotlin("test"))
}


application {
    mainClass.set("com.github.flemming_n_larsen.mermaid_class_diagram_generator.AppKt")
}

tasks.test {
    useJUnitPlatform()
}