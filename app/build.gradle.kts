plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
}

application {
    mainClass.set("com.github.flemming_n_larsen.mermaid_class_diagram_generator.AppKt")
}
