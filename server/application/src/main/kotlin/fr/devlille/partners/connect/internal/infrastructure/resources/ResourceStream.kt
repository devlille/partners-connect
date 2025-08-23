package fr.devlille.partners.connect.internal.infrastructure.resources

import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys

fun readResourceFile(path: String): String {
    val resource = object {}.javaClass.getResourceAsStream(path)
        ?: throw ForbiddenException(
            code = ErrorCode.FILE_NOT_FOUND,
            message = "Missing resource for path $path",
            meta = mapOf(
                MetaKeys.RESOURCE_PATH to path,
            ),
        )
    return resource.bufferedReader().use { it.readText() }
}
