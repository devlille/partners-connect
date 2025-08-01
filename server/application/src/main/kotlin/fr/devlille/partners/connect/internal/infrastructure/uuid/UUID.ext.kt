package fr.devlille.partners.connect.internal.infrastructure.uuid

import io.ktor.server.plugins.BadRequestException
import java.util.UUID

fun String.toUUID(): UUID = try {
    UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    throw BadRequestException("Invalid UUID format: $this")
}
