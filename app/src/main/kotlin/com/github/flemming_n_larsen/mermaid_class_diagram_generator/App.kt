package com.github.flemming_n_larsen.mermaid_class_diagram_generator

class App {
    val greeting: String
        get() {
            return "Hello World!"
        }
}

fun main() {
    println(App().greeting)
}
