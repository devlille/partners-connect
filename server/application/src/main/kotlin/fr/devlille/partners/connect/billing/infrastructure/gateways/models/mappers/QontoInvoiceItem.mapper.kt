package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoMoneyAmount
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail

@Suppress("SpreadOperator")
internal fun invoiceItems(
    partnership: PartnershipDetail,
): List<QontoInvoiceItem> {
    val pack = partnership.validatedPack ?: error("Partnership ${partnership.id} has no validated pack")
    return listOf(
        QontoInvoiceItem(
            title = "Sponsoring ${pack.name}",
            quantity = "1",
            unitPrice = QontoMoneyAmount(
                value = "${pack.packPriceOverride ?: pack.basePrice}",
                currency = partnership.currency,
            ),
            vatRate = "0",
        ),
        *pack.optionalOptions.map { option ->
            QontoInvoiceItem(
                title = option.labelWithValue,
                quantity = "${option.quantity}",
                unitPrice = QontoMoneyAmount(
                    value = "${option.priceOverride ?: option.price}",
                    currency = partnership.currency,
                ),
                vatRate = "0",
            )
        }.toTypedArray(),
    )
}
