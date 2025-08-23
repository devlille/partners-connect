package fr.devlille.partners.connect.internal.infrastructure.ktor

import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import io.ktor.http.content.PartData
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.ByteArrayOutputStream

fun PartData?.asByteArray(): ByteArray {
    if (this !is PartData.FileItem) {
        throw BadRequestException(
            message = "PartData is not a file",
            meta = mapOf(MetaKeys.EXPECTED_FORMAT to "file"),
        )
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
