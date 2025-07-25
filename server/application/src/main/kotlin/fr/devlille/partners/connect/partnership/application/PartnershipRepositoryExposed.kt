package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getPackById
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipRepositoryExposed(
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
) : PartnershipRepository {
    override fun register(eventId: String, companyId: String, register: RegisterPartnership): String = transaction {
        val eventUUID = UUID.fromString(eventId)
        EventEntity.findById(eventUUID)
            ?: throw NotFoundException("Event $eventId not found")

        val companyUUID = UUID.fromString(companyId)
        CompanyEntity.findById(companyUUID)
            ?: throw NotFoundException("Company $companyId not found")

        val packUUID = UUID.fromString(register.packId)
        SponsoringPackEntity.findById(packUUID)
            ?: throw NotFoundException("Pack ${register.packId} not found")

        val existing = PartnershipEntity
            .find { (PartnershipsTable.eventId eq eventUUID) and (PartnershipsTable.companyId eq companyUUID) }
            .singleOrNull()
        if (existing != null) {
            throw BadRequestException("Company already subscribed to this event")
        }

        val partnership = PartnershipEntity.new {
            this.eventId = eventUUID
            this.companyId = companyUUID
            selectedPackId = packUUID
            language = register.language
            phone = register.phone
        }

        register.emails.forEach {
            PartnershipEmailEntity.new {
                this.partnership = partnership
                this.email = it
            }
        }

        val optionalOptionIds = PackOptionsTable
            .selectAll()
            .where { (PackOptionsTable.pack eq packUUID) and (PackOptionsTable.required eq false) }
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = register.optionIds.map { UUID.fromString(it) }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }

        if (unknownOptions.isNotEmpty()) {
            throw BadRequestException("Some options are not optional in the selected pack: $unknownOptions")
        }

        optionsUUID.forEach {
            SponsoringOptionEntity.findById(it) ?: throw NotFoundException("Option $it not found")

            val hasTranslation = OptionTranslationEntity
                .find {
                    (OptionTranslationsTable.option eq it) and (OptionTranslationsTable.language eq register.language)
                }
                .empty().not()

            if (!hasTranslation) {
                throw BadRequestException("Option $it does not have a translation for language ${register.language}")
            }
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.optionId = it
            }
        }

        partnership.id.value.toString()
    }

    override fun getById(eventId: String, partnershipId: String): Partnership = transaction {
        val eventUUID = UUID.fromString(eventId)
        val partnership = findPartnership(eventId, partnershipId)
        Partnership(
            id = partnership.id.value.toString(),
            language = partnership.language,
            phone = partnership.phone,
            emails = PartnershipEmailEntity
                .find { PartnershipEmailsTable.partnershipId eq partnership.id }
                .map { it.email },
            selectedPack = partnership.selectedPackId?.let {
                val pack = packEntity.getPackById(eventUUID, it)
                pack.toDomain(
                    language = partnership.language,
                    requiredOptionIds = PackOptionsTable.getOptionsByPack(it)
                        .filter { it[PackOptionsTable.required] }
                        .map { it[PackOptionsTable.option].value },
                    optionalOptions = PackOptionsTable.getOptionsByPack(it)
                        .filterNot { it[PackOptionsTable.required] }
                        .map { it[PackOptionsTable.option].value },
                )
            },
            suggestionPack = partnership.suggestionPackId?.let {
                val pack = packEntity.getPackById(eventUUID, it)
                pack.toDomain(
                    language = partnership.language,
                    requiredOptionIds = PackOptionsTable.getOptionsByPack(it)
                        .filter { it[PackOptionsTable.required] }
                        .map { it[PackOptionsTable.option].value },
                    optionalOptions = PackOptionsTable.getOptionsByPack(it)
                        .filterNot { it[PackOptionsTable.required] }
                        .map { it[PackOptionsTable.option].value },
                )
            },
        )
    }

    override fun validate(eventId: String, partnershipId: String): String = transaction {
        val partnership = findPartnership(eventId, partnershipId)
        partnership.validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value.toString()
    }

    override fun decline(eventId: String, partnershipId: String): String = transaction {
        val partnership = findPartnership(eventId, partnershipId)
        partnership.declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value.toString()
    }

    private fun findPartnership(eventId: String, partnershipId: String): PartnershipEntity {
        val partnershipIdUUID = UUID.fromString(partnershipId)
        val eventUUID = UUID.fromString(eventId)
        return PartnershipEntity
            .find { (PartnershipsTable.id eq partnershipIdUUID) and (PartnershipsTable.eventId eq eventUUID) }
            .singleOrNull()
            ?: throw NotFoundException("Partnership not found")
    }
}
