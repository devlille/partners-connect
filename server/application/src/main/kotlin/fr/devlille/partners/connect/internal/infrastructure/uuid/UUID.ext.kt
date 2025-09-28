package fr.devlille.partners.connect.internal.infrastructure.uuid

import io.ktor.server.plugins.ParameterConversionException
import java.util.UUID

fun String.toUUID(): UUID = try {
    UUID.fromString(this)
} catch (ex: IllegalArgumentException) {
    throw ParameterConversionException(parameterName = "id", type = "UUID", cause = ex)
}
