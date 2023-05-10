package util

import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths

fun resourcePath(filename: String): Path =
    Paths.get(Paths::javaClass.javaClass.getResource(filename)?.toURI() ?: throw FileNotFoundException(filename))
