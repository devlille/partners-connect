package fr.devlille.partners.connect.internal.infrastructure.ktor

import io.ktor.http.content.PartData
import io.ktor.server.plugins.BadRequestException
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.ByteArrayOutputStream

fun PartData?.asByteArray(): ByteArray {
    if (this !is PartData.FileItem) {
        throw BadRequestException("PartData is not a file")
    }
    try {
        val output = ByteArrayOutputStream()
        this.provider().toInputStream().use { input ->
            output.buffered().use { output ->
                input.copyTo(output)
            }
        }
        return output.toByteArray()
    } finally {
        this.dispose()
    }
}
