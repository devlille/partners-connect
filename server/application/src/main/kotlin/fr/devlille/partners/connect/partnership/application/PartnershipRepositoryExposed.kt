package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.ContactInfo
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
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.format.DateTimeFormatter
import java.util.UUID

class PartnershipRepositoryExposed(
    private val partnershipEntity: UUIDEntityClass<PartnershipEntity> = PartnershipEntity,
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
    private val translationEntity: UUIDEntityClass<OptionTranslationEntity> = OptionTranslationEntity,
    private val packOptionTable: PackOptionsTable = PackOptionsTable,
) : PartnershipRepository {
    override fun register(eventId: UUID, register: RegisterPartnership): UUID = transaction {
        val event = EventEntity.findById(eventId)
            ?: throw NotFoundException("Event $eventId not found")
        val companyId = register.companyId.toUUID()
        val company = CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")
        val pack = packEntity.findById(register.packId.toUUID())
            ?: throw NotFoundException("Pack ${register.packId} not found")

        val existing = partnershipEntity.singleByEventAndCompany(eventId, companyId)
        if (existing != null) {
            throw BadRequestException("Company already subscribed to this event")
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
            throw BadRequestException("Some options are not optional in the selected pack: $unknownOptions")
        }

        optionsUUID.forEach {
            val option = SponsoringOptionEntity.findById(it) ?: throw NotFoundException("Option $it not found")

            val noTranslations = translationEntity
                .listTranslationsByOptionAndLanguage(it, register.language)
                .isEmpty()

            if (noTranslations) {
                throw BadRequestException("Option $it does not have a translation for language ${register.language}")
            }
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.packId = pack.id
                this.option = option
            }
        }

        partnership.id.value
    }

    override fun getById(eventId: UUID, partnershipId: UUID): Partnership = transaction {
        val partnership = findPartnership(eventId, partnershipId)
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

    override fun getCompanyByPartnershipId(eventId: UUID, partnershipId: UUID): Company = transaction {
        findPartnership(eventId, partnershipId).company.toDomain()
    }

    override fun validate(eventId: UUID, partnershipId: UUID): UUID = transaction {
        val partnership = findPartnership(eventId, partnershipId)
        partnership.validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventId: UUID, partnershipId: UUID): UUID = transaction {
        val partnership = findPartnership(eventId, partnershipId)
        partnership.declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun listByEvent(
        eventId: UUID,
        filters: PartnershipFilters,
        sort: String,
        direction: String,
    ): List<PartnershipItem> = transaction {
        val allPartnerships = PartnershipEntity.find { PartnershipsTable.eventId eq eventId }
        val filteredPartnerships = allPartnerships.filter { partnership ->
            matchesBasicFilters(partnership, filters) &&
                matchesAdvancedFilters(partnership, filters, eventId)
        }
        filteredPartnerships.map { partnership -> mapToPartnershipItem(partnership) }
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
        eventId: UUID
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
            contact = ContactInfo(
                displayName = partnership.contactName,
                role = partnership.contactRole,
            ),
            companyName = partnership.company.name,
            packName = partnership.selectedPack?.name ?: "Unknown",
            suggestedPackName = partnership.suggestionPack?.name,
            language = partnership.language,
            phone = partnership.phone,
            emails = emails,
            createdAt = partnership.createdAt.toJavaLocalDateTime()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
    }

    private fun findPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity = partnershipEntity
        .singleByEventAndPartnership(eventId, partnershipId)
        ?: throw NotFoundException("Partnership not found")
}
