package fr.devlille.partners.connect.internal.infrastructure.resources

import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException

fun readResourceFile(path: String): String {
    val resource = object {}.javaClass.getResourceAsStream(path)
        ?: throw ForbiddenException(
            code = ErrorCode.FILE_NOT_FOUND,
            message = "Missing resource for path $path",
            meta = mapOf(
                "resourcePath" to path,
                "action" to "read",
            ),
        )
    return resource.bufferedReader().use { it.readText() }
}
