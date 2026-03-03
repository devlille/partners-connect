package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.partnershipId: UUID
    get() = getValue("partnershipId").toUUID()

val StringValues.ticketId: String
    get() = getValue("ticketId")

val StringValues.billingStatus: InvoiceStatus
    get() {
        val statusParam = getValue("billingStatus")
        return runCatching { InvoiceStatus.valueOf(statusParam.uppercase()) }
            .getOrElse {
                throw ParameterConversionException(parameterName = "billingStatus", type = "InvoiceStatus", cause = it)
            }
    }

/**
 * Parses a nullable query parameter string as a strict boolean.
 *
 * Returns [default] when the value is null.
 * Returns `true` for the string "true" (case-insensitive).
 * Returns `false` for the string "false" (case-insensitive).
 * Throws [BadRequestException] for any other non-null, non-empty value.
 *
 * @param paramName The parameter name used in the error message for diagnostics.
 * @param default The value to return when the parameter is absent (null).
 */
fun String?.toBooleanStrict(paramName: String, default: Boolean): Boolean {
    if (this == null) return default
    return when (this.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw BadRequestException("Invalid value for '$paramName': expected 'true' or 'false', got '$this'")
    }
}
