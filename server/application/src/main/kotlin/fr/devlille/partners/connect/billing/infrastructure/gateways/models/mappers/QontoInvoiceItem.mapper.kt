package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoMoneyAmount
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing

@Suppress("SpreadOperator")
internal fun invoiceItems(
    pricing: PartnershipPricing,
): List<QontoInvoiceItem> = listOf(
    QontoInvoiceItem(
        title = "Sponsoring ${pricing.packName}",
        quantity = "1",
        unitPrice = QontoMoneyAmount(value = "${pricing.basePrice}", currency = pricing.currency),
        vatRate = "0",
    ),
    *pricing.optionalOptions.map { option ->
        QontoInvoiceItem(
            title = option.label,
            quantity = "${option.quantity}",
            unitPrice = QontoMoneyAmount(value = "${option.unitAmount}", currency = pricing.currency),
            vatRate = "0",
        )
    }.toTypedArray(),
)
