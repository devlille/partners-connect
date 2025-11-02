package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listTranslationsByOptionAndLanguage
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipSuggestionRepositoryExposed : PartnershipSuggestionRepository {
    override fun suggest(
        eventSlug: String,
        partnershipId: UUID,
        input: SuggestPartnership,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        val suggestedPack = SponsoringPackEntity.findById(input.packId.toUUID())
            ?: throw NotFoundException("Pack ${input.packId} not found")
        val optionalOptionIds = PackOptionsTable
            .listOptionalOptionsByPack(suggestedPack.id.value)
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = input.optionSelections.map { it.optionId.toUUID() }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }
        if (unknownOptions.isNotEmpty()) {
            throw ForbiddenException("Some options are not optional in the suggested pack: $unknownOptions")
        }

        // Remove previous suggested options
        PartnershipOptionEntity.deleteAllByPartnershipId(partnership.id.value, suggestedPack.id.value)

        input.optionSelections.forEach { selection ->
            val optionId = selection.optionId.toUUID()
            val option = SponsoringOptionEntity.findById(optionId)
                ?: throw NotFoundException("Option $optionId not found")
            val noTranslation = OptionTranslationEntity
                .listTranslationsByOptionAndLanguage(optionId, partnership.language)
                .isEmpty()
            if (noTranslation) {
                throw ForbiddenException(
                    "Option $optionId doesn't have a translation for language ${partnership.language}",
                )
            }

            // Create partnership option with selection data based on selection type
            PartnershipOptionEntity.create(selection, partnership, suggestedPack, option)
        }

        partnership.suggestionPack = suggestedPack
        partnership.suggestionSentAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.suggestionApprovedAt = null
        partnership.suggestionDeclinedAt = null

        partnership.id.value
    }

    override fun approve(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.suggestionApprovedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.suggestionDeclinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }
}
