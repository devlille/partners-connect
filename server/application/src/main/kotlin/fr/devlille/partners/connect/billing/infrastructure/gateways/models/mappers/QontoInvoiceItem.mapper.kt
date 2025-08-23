package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoMoneyAmount
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity

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
            ?: throw NotFoundException(
                code = ErrorCode.TRANSLATION_NOT_FOUND,
                message = "Translation not found for option ${option.id} in language $language",
                meta = mapOf(
                    MetaKeys.ID to option.id.value.toString(),
                    MetaKeys.LANGUAGE to language,
                ),
            )
        QontoInvoiceItem(
            title = translation.name,
            quantity = "1",
            unitPrice = QontoMoneyAmount(value = "${option.price}", currency = "EUR"),
            vatRate = "0",
        )
    }.toTypedArray(),
)
