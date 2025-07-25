package fr.devlille.partners.connect.internal.infrastructure.resources

fun readResourceFile(path: String): String {
    val resource = object {}.javaClass.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Missing resource for path $path")
    return resource.bufferedReader().use { it.readText() }
}
