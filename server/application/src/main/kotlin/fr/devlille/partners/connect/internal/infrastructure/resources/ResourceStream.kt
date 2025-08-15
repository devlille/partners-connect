package fr.devlille.partners.connect.internal.infrastructure.resources

import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException

fun readResourceFile(path: String): String {
    val resource = object {}.javaClass.getResourceAsStream(path)
        ?: throw ForbiddenException("Missing resource for path $path")
    return resource.bufferedReader().use { it.readText() }
}
