package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.application.mappers.toDetailedDomain
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listTranslationsByOptionAndLanguage
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipRepositoryExposed : PartnershipRepository {
    @Suppress("LongMethod")
    override fun register(eventSlug: String, register: RegisterPartnership): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val companyId = register.companyId.toUUID()
        val company = CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")
        val pack = SponsoringPackEntity.findById(register.packId.toUUID())
            ?: throw NotFoundException("Pack ${register.packId} not found")

        val existing = PartnershipEntity.singleByEventAndCompany(event.id.value, companyId)
        if (existing != null) {
            throw ConflictException("Company already subscribed to this event")
        }

        val partnership = PartnershipEntity.new {
            this.event = event
            this.company = company
            this.selectedPack = pack
            this.phone = register.phone
            this.contactName = register.contactName
            this.contactRole = register.contactRole
            this.language = register.language
        }

        register.emails.forEach {
            PartnershipEmailEntity.new {
                this.partnership = partnership
                this.email = it
            }
        }

        val options = PackOptionsTable.listOptionsByPack(pack.id.value)
        val optionalOptionIds = options
            .filter { it[PackOptionsTable.required].not() }
            .map { it[PackOptionsTable.option].value }
        val requiredOptionIds = options
            .filter { it[PackOptionsTable.required] }
            .map { it[PackOptionsTable.option].value }

        val unknownOptions = register.optionSelections
            .map { it.optionId.toUUID() }
            .filterNot { it in optionalOptionIds }
        if (unknownOptions.isNotEmpty()) {
            throw ForbiddenException("Some options are not optional in the selected pack: $unknownOptions")
        }

        requiredOptionIds.forEach { optionId ->
            val option = SponsoringOptionEntity.findById(optionId)
                ?: throw NotFoundException("Option $optionId not found")
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.option = option
                this.packId = pack.id
            }
        }

        register.optionSelections.forEach { selection ->
            val optionId = selection.optionId.toUUID()
            val option = SponsoringOptionEntity.findById(optionId)
                ?: throw NotFoundException("Option $optionId not found")
            val noTranslations = OptionTranslationEntity
                .listTranslationsByOptionAndLanguage(optionId, register.language)
                .isEmpty()
            if (noTranslations) {
                throw ForbiddenException(
                    "Option $optionId does not have a translation for language ${register.language}",
                )
            }
            PartnershipOptionEntity.create(selection, partnership, pack, option)
        }

        partnership.id.value
    }

    override fun getById(eventSlug: String, partnershipId: UUID): Partnership = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        Partnership(
            id = partnership.id.value.toString(),
            language = partnership.language,
            phone = partnership.phone,
            emails = PartnershipEmailEntity
                .find { PartnershipEmailsTable.partnershipId eq partnership.id }
                .map { it.email },
            selectedPack = partnership.selectedPack?.let { pack ->
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, pack.id.value)
                        .map { it.id.value },
                )
            },
            suggestionPack = partnership.suggestionPack?.let { pack ->
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, pack.id.value)
                        .map { it.id.value },
                )
            },
        )
    }

    override fun getByIdDetailed(eventSlug: String, partnershipId: UUID): PartnershipDetail = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        val billing = BillingEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)

        partnership.toDetailedDomain(
            billing = billing,
            selectedPack = partnership.selectedPack?.let { pack ->
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, pack.id.value)
                        .map { it.id.value },
                )
            },
            suggestionPack = partnership.suggestionPack?.let { pack ->
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, pack.id.value)
                        .map { it.id.value },
                )
            },
            validatedPack = partnership.validatedPack()?.let { pack ->
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, pack.id.value)
                        .map { it.id.value },
                )
            },
        )
    }

    override fun getCompanyByPartnershipId(eventSlug: String, partnershipId: UUID): Company = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.company.toDomain()
    }

    override fun listByEvent(
        eventSlug: String,
        filters: PartnershipFilters,
        direction: String,
    ): List<PartnershipItem> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val sort = if (direction == "asc") SortOrder.ASC else SortOrder.DESC
        val partnerships = PartnershipEntity
            .filters(
                eventId = eventId,
                packId = filters.packId?.toUUID(),
                validated = filters.validated,
                suggestion = filters.suggestion,
                agreementGenerated = filters.agreementGenerated,
                agreementSigned = filters.agreementSigned,
            )
            .orderBy(PartnershipsTable.createdAt to sort)
        val filteredPartnerships = if (filters.paid != null) {
            partnerships.filter {
                val billing = BillingEntity.singleByEventAndPartnership(eventId, it.id.value)
                if (filters.paid) billing?.status == InvoiceStatus.PAID else billing?.status != InvoiceStatus.PAID
            }
        } else {
            partnerships
        }
        filteredPartnerships.map { partnership ->
            partnership.toDomain(PartnershipEmailEntity.emails(partnership.id.value))
        }
    }

    override fun listByCompany(companyId: UUID): List<PartnershipItem> = transaction {
        CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")
        PartnershipEntity
            .find { PartnershipsTable.companyId eq companyId }
            .orderBy(PartnershipsTable.createdAt to SortOrder.DESC)
            .map { partnership ->
                partnership.toDomain(PartnershipEmailEntity.emails(partnership.id.value))
            }
    }
}
