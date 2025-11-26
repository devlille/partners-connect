package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceSettings
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoLegalCapitalShare
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoPaymentMethods
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun EventEntity.toQontoInvoiceRequest(
    clientId: String,
    invoicePo: String?,
    invoiceItems: List<QontoInvoiceItem>,
): QontoInvoiceRequest {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val eventMonth = "%02d".format(startTime.monthNumber)
    val eventDay = "%02d".format(startTime.dayOfMonth)

    // Validate required fields for invoice generation
    val requiredIban = transaction { organisation.iban }
        ?: throw ForbiddenException("Field iban is required to perform this operation.")

    return QontoInvoiceRequest(
        settings = QontoInvoiceSettings(legalCapitalShare = QontoLegalCapitalShare(currency = "EUR")),
        clientId = clientId,
        issueDate = "${now.year}-${"%02d".format(now.monthNumber)}-${"%02d".format(now.dayOfMonth)}",
        dueDate = "${startTime.year}-$eventMonth-$eventDay",
        currency = "EUR",
        paymentMethods = QontoPaymentMethods(iban = requiredIban),
        purchaseOrder = invoicePo,
        items = invoiceItems,
    )
}
