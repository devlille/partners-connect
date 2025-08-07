package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoMoneyAmount
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import io.ktor.server.plugins.NotFoundException

@Suppress("SpreadOperator")
internal fun invoiceItems(
    language: String,
    pack: SponsoringPackEntity,
    options: List<SponsoringOptionEntity>,
): List<QontoInvoiceItem> = listOf(
    QontoInvoiceItem(
        title = "Sponsoring ${pack.name}",
        quantity = "1",
        unitPrice = QontoMoneyAmount(value = "${pack.basePrice}", currency = "EUR"),
        vatRate = "0",
    ),
    *options.map { option ->
        val translation = option.translations.firstOrNull { it.language == language }
            ?: throw NotFoundException("Translation not found for option ${option.id} in language $language")
        QontoInvoiceItem(
            title = translation.name,
            quantity = "1",
            unitPrice = QontoMoneyAmount(value = "${option.price}", currency = "EUR"),
            vatRate = "0",
        )
    }.toTypedArray(),
)
