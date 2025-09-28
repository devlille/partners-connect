package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.CommunicationItem
import fr.devlille.partners.connect.partnership.domain.CommunicationPlan
import fr.devlille.partners.connect.partnership.domain.Contact
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.listByPartnershipAndPack
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndCompany
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listTranslationsByOptionAndLanguage
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("TooManyFunctions") // Required to support both listByEvent and listByCompany methods
class PartnershipRepositoryExposed(
    private val partnershipEntity: UUIDEntityClass<PartnershipEntity> = PartnershipEntity,
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
    private val translationEntity: UUIDEntityClass<OptionTranslationEntity> = OptionTranslationEntity,
    private val packOptionTable: PackOptionsTable = PackOptionsTable,
) : PartnershipRepository {
    override fun register(eventSlug: String, register: RegisterPartnership): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val companyId = register.companyId.toUUID()
        val company = CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")
        val pack = packEntity.findById(register.packId.toUUID())
            ?: throw NotFoundException("Pack ${register.packId} not found")

        val existing = partnershipEntity.singleByEventAndCompany(eventId, companyId)
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

        val optionalOptionIds = packOptionTable
            .listOptionalOptionsByPack(pack.id.value)
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = register.optionIds.map { it.toUUID() }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }

        if (unknownOptions.isNotEmpty()) {
            throw ForbiddenException("Some options are not optional in the selected pack: $unknownOptions")
        }

        optionsUUID.forEach {
            val option = SponsoringOptionEntity.findById(it) ?: throw NotFoundException("Option $it not found")

            val noTranslations = translationEntity
                .listTranslationsByOptionAndLanguage(it, register.language)
                .isEmpty()

            if (noTranslations) {
                throw ForbiddenException("Option $it does not have a translation for language ${register.language}")
            }
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.packId = pack.id
                this.option = option
            }
        }

        partnership.id.value
    }

    override fun getById(eventSlug: String, partnershipId: UUID): Partnership = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
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

    override fun getCompanyByPartnershipId(eventSlug: String, partnershipId: UUID): Company = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        findPartnership(event.id.value, partnershipId).company.toDomain()
    }

    override fun validate(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun listByEvent(
        eventSlug: String,
        filters: PartnershipFilters,
        sort: String,
        direction: String,
    ): List<PartnershipItem> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val allPartnerships = PartnershipEntity.find { PartnershipsTable.eventId eq eventId }
        val filteredPartnerships = allPartnerships.filter { partnership ->
            matchesBasicFilters(partnership, filters) &&
                matchesAdvancedFilters(partnership, filters, eventId)
        }
        filteredPartnerships.map { partnership -> mapToPartnershipItem(partnership) }
    }

    override fun listByCompany(companyId: UUID): List<PartnershipItem> = transaction {
        // Check if the company exists first
        CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")

        PartnershipEntity
            .find { PartnershipsTable.companyId eq companyId }
            .orderBy(PartnershipsTable.createdAt to SortOrder.DESC)
            .map { partnership ->
                val emails = PartnershipEmailEntity
                    .find { PartnershipEmailsTable.partnershipId eq partnership.id }
                    .map { it.email }

                PartnershipItem(
                    id = partnership.id.value.toString(),
                    contact = Contact(
                        displayName = partnership.contactName,
                        role = partnership.contactRole,
                    ),
                    companyName = partnership.company.name,
                    eventName = partnership.event.name,
                    packName = partnership.selectedPack?.name,
                    suggestedPackName = partnership.suggestionPack?.name,
                    language = partnership.language,
                    phone = partnership.phone,
                    emails = emails,
                    createdAt = partnership.createdAt,
                )
            }
    }

    private fun matchesBasicFilters(partnership: PartnershipEntity, filters: PartnershipFilters): Boolean {
        val packMatches = filters.packId?.let {
            partnership.selectedPack?.id?.value == it.toUUID()
        } ?: true

        val validatedMatches = filters.validated?.let {
            if (it) partnership.validatedAt != null else partnership.validatedAt == null
        } ?: true

        val suggestionMatches = filters.suggestion?.let {
            if (it) partnership.suggestionPack != null else partnership.suggestionPack == null
        } ?: true

        return packMatches && validatedMatches && suggestionMatches
    }

    private fun matchesAdvancedFilters(
        partnership: PartnershipEntity,
        filters: PartnershipFilters,
        eventId: UUID,
    ): Boolean {
        val paidMatches = filters.paid?.let {
            val billing = BillingEntity.singleByEventAndPartnership(eventId, partnership.id.value)
            if (it) billing?.status == InvoiceStatus.PAID else billing?.status != InvoiceStatus.PAID
        } ?: true

        val agreementGeneratedMatches = filters.agreementGenerated?.let {
            if (it) partnership.agreementUrl != null else partnership.agreementUrl == null
        } ?: true

        val agreementSignedMatches = filters.agreementSigned?.let {
            if (it) partnership.agreementSignedUrl != null else partnership.agreementSignedUrl == null
        } ?: true

        return paidMatches && agreementGeneratedMatches && agreementSignedMatches
    }

    private fun mapToPartnershipItem(partnership: PartnershipEntity): PartnershipItem {
        val emails = PartnershipEmailEntity
            .find { PartnershipEmailsTable.partnershipId eq partnership.id }
            .map { it.email }

        return PartnershipItem(
            id = partnership.id.toString(),
            contact = Contact(
                displayName = partnership.contactName,
                role = partnership.contactRole,
            ),
            companyName = partnership.company.name,
            packName = partnership.selectedPack?.name,
            suggestedPackName = partnership.suggestionPack?.name,
            eventName = partnership.event.name,
            language = partnership.language,
            phone = partnership.phone,
            emails = emails,
            createdAt = partnership.createdAt,
        )
    }

    private fun findPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity = partnershipEntity
        .singleByEventAndPartnership(eventId, partnershipId)
        ?: throw NotFoundException("Partnership not found")

    override fun updateBoothLocation(
        eventSlug: String,
        partnershipId: UUID,
        location: String,
    ): Unit = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Check if location is already taken by another partnership for this event
        val existingPartnership = partnershipEntity.find {
            (PartnershipsTable.eventId eq event.id) and
                (PartnershipsTable.boothLocation eq location) and
                (PartnershipsTable.id neq partnershipId)
        }.firstOrNull()

        if (existingPartnership != null) {
            val companyName = existingPartnership.company.name
            throw ForbiddenException(
                "Location '$location' is already assigned to another partnership " +
                    "for this event by company '$companyName'",
            )
        }

        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.boothLocation = location
    }

    override fun updateCommunicationPublicationDate(
        eventSlug: String,
        partnershipId: UUID,
        publicationDate: LocalDateTime,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.communicationPublicationDate = publicationDate
        partnership.id.value
    }

    override fun updateCommunicationSupportUrl(
        eventSlug: String,
        partnershipId: UUID,
        supportUrl: String,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.communicationSupportUrl = supportUrl
        partnership.id.value
    }

    override fun listCommunicationPlan(eventSlug: String): CommunicationPlan = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val eventId = event.id.value
        val partnerships = PartnershipEntity.find { PartnershipsTable.eventId eq eventId }

        val communicationItems = partnerships.map { partnership ->
            CommunicationItem(
                partnershipId = partnership.id.value.toString(),
                companyName = partnership.company.name,
                publicationDate = partnership.communicationPublicationDate,
                supportUrl = partnership.communicationSupportUrl,
            )
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val done = communicationItems
            .filter { it.publicationDate != null && it.publicationDate < now }
            .sortedByDescending { it.publicationDate }

        val planned = communicationItems
            .filter { it.publicationDate != null && it.publicationDate >= now }
            .sortedBy { it.publicationDate }

        val unplanned = communicationItems
            .filter { it.publicationDate == null }
            .sortedBy { it.companyName }

        CommunicationPlan(done = done, planned = planned, unplanned = unplanned)
    }
}
