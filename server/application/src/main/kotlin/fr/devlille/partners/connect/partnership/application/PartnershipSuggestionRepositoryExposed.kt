package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.deleteAllByPartnershipId
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndCompanyAndPartnership
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
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipSuggestionRepositoryExposed(
    private val partnershipEntity: UUIDEntityClass<PartnershipEntity> = PartnershipEntity,
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
    private val partnershipOptionEntity: UUIDEntityClass<PartnershipOptionEntity> = PartnershipOptionEntity,
    private val translationEntity: UUIDEntityClass<OptionTranslationEntity> = OptionTranslationEntity,
    private val packOptionTable: PackOptionsTable = PackOptionsTable,
) : PartnershipSuggestionRepository {
    override fun suggest(
        eventId: UUID,
        companyId: UUID,
        partnershipId: UUID,
        input: SuggestPartnership,
    ): UUID = transaction {
        val partnership = partnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")
        if (partnership.eventId != eventId || partnership.companyId != companyId) {
            throw BadRequestException("Mismatch between path and partnership")
        }
        val suggestedPack = packEntity.findById(input.packId.toUUID())
            ?: throw NotFoundException("Pack ${input.packId} not found")

        val optionalOptionIds = packOptionTable
            .listOptionalOptionsByPack(suggestedPack.id.value)
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = input.optionIds.map { it.toUUID() }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }
        if (unknownOptions.isNotEmpty()) {
            throw BadRequestException("Some options are not optional in the suggested pack: $unknownOptions")
        }

        // Remove previous suggested options
        partnershipOptionEntity.deleteAllByPartnershipId(partnership.id.value, suggestedPack.id.value)

        optionsUUID.forEach {
            val option = SponsoringOptionEntity.findById(it) ?: throw NotFoundException("Option $it not found")
            val noTranslation = translationEntity
                .listTranslationsByOptionAndLanguage(it, partnership.language)
                .isEmpty()
            if (noTranslation) {
                throw BadRequestException("Option $it does not have a translation for language ${partnership.language}")
            }
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.packId = suggestedPack.id
                this.optionId = option.id
            }
        }

        partnership.suggestionPackId = suggestedPack.id.value
        partnership.suggestionSentAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.suggestionApprovedAt = null
        partnership.suggestionDeclinedAt = null

        partnership.id.value
    }

    override fun approve(eventId: UUID, companyId: UUID, partnershipId: UUID): UUID = transaction {
        val partnership = findPartnership(eventId, companyId, partnershipId)
        partnership.suggestionApprovedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventId: UUID, companyId: UUID, partnershipId: UUID): UUID = transaction {
        val partnership = findPartnership(eventId, companyId, partnershipId)
        partnership.suggestionDeclinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    private fun findPartnership(eventId: UUID, companyId: UUID, partnershipId: UUID): PartnershipEntity =
        partnershipEntity
            .singleByEventAndCompanyAndPartnership(eventId, companyId, partnershipId)
            ?: throw NotFoundException("Partnership not found")
}
