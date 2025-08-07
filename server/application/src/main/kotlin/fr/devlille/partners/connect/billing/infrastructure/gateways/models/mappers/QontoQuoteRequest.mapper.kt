package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoQuoteRequest
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun EventEntity.toQontoQuoteRequest(
    clientId: String,
    invoiceItems: List<QontoInvoiceItem>,
): QontoQuoteRequest {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val eventMonth = "%02d".format(startTime.monthNumber)
    val eventDay = "%02d".format(startTime.dayOfMonth)
    return QontoQuoteRequest(
        clientId = clientId,
        issueDate = "${startTime.year}-$eventMonth-$eventDay",
        expiryDate = "${now.year}-${"%02d".format(now.monthNumber)}-${"%02d".format(now.dayOfMonth)}",
        currency = "EUR",
        items = invoiceItems,
        termsAndConditions = " ",
    )
}
