package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.deleteAllByPartnershipId
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listTranslationsByOptionAndLanguage
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
        eventSlug: String,
        partnershipId: UUID,
        input: SuggestPartnership,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
            )
        val partnership = findPartnership(event.id.value, partnershipId)
        val suggestedPack = packEntity.findById(input.packId.toUUID())
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Pack ${input.packId} not found",
            )
        val optionalOptionIds = packOptionTable
            .listOptionalOptionsByPack(suggestedPack.id.value)
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = input.optionIds.map { it.toUUID() }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }
        if (unknownOptions.isNotEmpty()) {
            throw BadRequestException(
                message = "Some options are not optional in the suggested pack: $unknownOptions",
            )
        }

        // Remove previous suggested options
        partnershipOptionEntity.deleteAllByPartnershipId(partnership.id.value, suggestedPack.id.value)

        optionsUUID.forEach {
            val option = SponsoringOptionEntity.findById(it) ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Option $it not found",
            )
            val noTranslation = translationEntity
                .listTranslationsByOptionAndLanguage(it, partnership.language)
                .isEmpty()
            if (noTranslation) {
                throw BadRequestException(
                    message = "Option $it does not have a translation for language ${partnership.language}",
                )
            }
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.packId = suggestedPack.id
                this.option = option
            }
        }

        partnership.suggestionPack = suggestedPack
        partnership.suggestionSentAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.suggestionApprovedAt = null
        partnership.suggestionDeclinedAt = null

        partnership.id.value
    }

    override fun approve(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
            )
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.suggestionApprovedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
            )
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.suggestionDeclinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    private fun findPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity =
        partnershipEntity
            .singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Partnership not found",
            )
}
