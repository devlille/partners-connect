package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.listByPartnershipAndPack
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndCompany
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listTranslationsByOptionAndLanguage
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipRepositoryExposed(
    private val partnershipEntity: UUIDEntityClass<PartnershipEntity> = PartnershipEntity,
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
    private val translationEntity: UUIDEntityClass<OptionTranslationEntity> = OptionTranslationEntity,
    private val packOptionTable: PackOptionsTable = PackOptionsTable,
) : PartnershipRepository {
    override fun register(eventId: UUID, companyId: UUID, register: RegisterPartnership): UUID = transaction {
        EventEntity.findById(eventId)
            ?: throw NotFoundException("Event $eventId not found")
        CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")
        val pack = packEntity.findById(register.packId.toUUID())
            ?: throw NotFoundException("Pack ${register.packId} not found")

        val existing = partnershipEntity.singleByEventAndCompany(eventId, companyId)
        if (existing != null) {
            throw BadRequestException("Company already subscribed to this event")
        }

        val partnership = PartnershipEntity.new {
            this.eventId = eventId
            this.companyId = companyId
            selectedPackId = pack.id.value
            language = register.language
            phone = register.phone
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
                this.optionId = option.id
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
            selectedPack = partnership.selectedPackId?.let { packId ->
                val pack = packEntity.singlePackById(eventId, packId)
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, packId)
                        .map { it.id.value },
                )
            },
            suggestionPack = partnership.suggestionPackId?.let { packId ->
                val pack = packEntity.singlePackById(eventId, packId)
                pack.toDomain(
                    language = partnership.language,
                    optionIds = PartnershipOptionEntity.listByPartnershipAndPack(partnershipId, packId)
                        .map { it.id.value },
                )
            },
        )
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

    private fun findPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity = partnershipEntity
        .singleByEventAndPartnership(eventId, partnershipId)
        ?: throw NotFoundException("Partnership not found")
}
