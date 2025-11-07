package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.OptionPricing
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipBillingRepositoryExposed : PartnershipBillingRepository {
    override fun getByPartnershipId(eventSlug: String, partnershipId: UUID): CompanyBillingData = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        BillingEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?.let(BillingEntity::toDomain)
            ?: throw NotFoundException("Billing not found for partnership ID: $partnershipId")
    }

    override fun createOrUpdate(eventSlug: String, partnershipId: UUID, input: CompanyBillingData): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val existing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
        if (existing == null) {
            val partnership = PartnershipEntity.singleByEventAndPartnership(eventId, partnershipId)
                ?: throw NotFoundException("Partnership not found")
            BillingEntity.new {
                this.event = partnership.event
                this.partnership = partnership
                this.name = input.name ?: partnership.company.name
                this.contactFirstName = input.contact.firstName
                this.contactLastName = input.contact.lastName
                this.contactEmail = input.contact.email
                this.po = input.po
                this.status = InvoiceStatus.PENDING
            }
        } else {
            existing.apply {
                this.name = input.name ?: existing.partnership.company.name
                this.contactFirstName = input.contact.firstName
                this.contactLastName = input.contact.lastName
                this.contactEmail = input.contact.email
                this.po = input.po ?: this.po
            }
        }.id.value
    }

    override fun updateInvoiceUrl(eventSlug: String, partnershipId: UUID, invoiceUrl: String): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val existing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
        existing.invoicePdfUrl = invoiceUrl
        existing.id.value
    }

    override fun updateQuoteUrl(eventSlug: String, partnershipId: UUID, quoteUrl: String): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val existing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Invoice not found for partnership ID: $partnershipId")
        existing.quotePdfUrl = quoteUrl
        existing.id.value
    }

    override fun updateStatus(eventSlug: String, partnershipId: UUID, status: InvoiceStatus): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val existing = BillingEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Billing not found")
        existing.status = status
        existing.id.value
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun computePricing(eventSlug: String, partnershipId: UUID): PartnershipPricing = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership with id $partnershipId not found")
        val pack = partnership.validatedPack()
            ?: throw NotFoundException("Partnership with id $partnershipId hasn't validated pack")
        val optionalOptionIds = PackOptionsTable
            .listOptionalOptionsByPack(pack.id.value)
            .map { it[PackOptionsTable.option].value }
        val options = PartnershipOptionEntity
            .listByPartnershipAndPack(partnershipId, pack.id.value)
        val language = partnership.language
        val optionsPricing = options.map {
            val option = it.option
            val label = option.translations.firstOrNull { it.language == language }
                ?: throw NotFoundException("Translation not found for option ${option.id} in language $language")
            val required = optionalOptionIds.contains(option.id.value).not()
            when (option.optionType) {
                OptionType.TEXT -> OptionPricing(
                    label = label.name,
                    amount = option.price ?: 0,
                    unitAmount = option.price ?: 0,
                    quantity = 1,
                    required = required,
                )
                OptionType.TYPED_QUANTITATIVE -> OptionPricing(
                    label = label.name,
                    amount = (option.price ?: 0) * (it.selectedQuantity ?: 0),
                    unitAmount = option.price ?: 0,
                    selectedValue = it.selectedQuantity?.toString(),
                    quantity = it.selectedQuantity ?: 0,
                    required = required,
                )

                OptionType.TYPED_NUMBER -> OptionPricing(
                    label = label.name,
                    amount = (option.price ?: 0) * (option.fixedQuantity ?: 0),
                    unitAmount = option.fixedQuantity ?: 0,
                    selectedValue = option.fixedQuantity?.toString(),
                    quantity = option.fixedQuantity ?: 0,
                    required = required,
                )

                OptionType.TYPED_SELECTABLE -> OptionPricing(
                    label = label.name,
                    amount = it.selectedValue?.price ?: option.price ?: 0,
                    unitAmount = it.selectedValue?.price ?: option.price ?: 0,
                    selectedValue = it.selectedValue?.value,
                    quantity = 1,
                    required = required,
                )
            }
        }
        val optionalOptions = optionsPricing.filter { it.required.not() }
        PartnershipPricing(
            eventId = event.id.value.toString(),
            partnershipId = partnershipId.toString(),
            packName = pack.name,
            basePrice = pack.basePrice,
            currency = "EUR",
            totalAmount = pack.basePrice + optionalOptions.map { it.amount }.sumOf { it },
            requiredOptions = optionsPricing.filter { it.required },
            optionalOptions = optionalOptions,
        )
    }
}
