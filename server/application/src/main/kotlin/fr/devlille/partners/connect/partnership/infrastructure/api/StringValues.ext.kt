package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
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
