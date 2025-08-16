package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.Contact
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
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
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
                    createdAt = partnership.createdAt.toString(),
                )
            }
    }

    private fun findPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity = partnershipEntity
        .singleByEventAndPartnership(eventId, partnershipId)
        ?: throw NotFoundException("Partnership not found")
}
