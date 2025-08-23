package fr.devlille.partners.connect.internal.infrastructure.uuid

import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import java.util.UUID

fun String.toUUID(): UUID = try {
    UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    throw BadRequestException(
        message = "Invalid UUID format: $this",
        meta = mapOf(MetaKeys.EXPECTED_FORMAT to "UUID", MetaKeys.FIELD to this),
    )
}
