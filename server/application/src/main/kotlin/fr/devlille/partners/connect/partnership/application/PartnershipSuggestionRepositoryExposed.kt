package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.NumberSelection
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.QuantitativeSelection
import fr.devlille.partners.connect.partnership.domain.SelectableSelection
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.domain.TextSelection
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
    @Suppress("LongMethod")
    override fun suggest(
        eventSlug: String,
        partnershipId: UUID,
        input: SuggestPartnership,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        val suggestedPack = packEntity.findById(input.packId.toUUID())
            ?: throw NotFoundException("Pack ${input.packId} not found")
        val optionalOptionIds = packOptionTable
            .listOptionalOptionsByPack(suggestedPack.id.value)
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = input.optionSelections.map { it.optionId.toUUID() }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }
        if (unknownOptions.isNotEmpty()) {
            throw ForbiddenException("Some options are not optional in the suggested pack: $unknownOptions")
        }

        // Remove previous suggested options
        partnershipOptionEntity.deleteAllByPartnershipId(partnership.id.value, suggestedPack.id.value)

        input.optionSelections.forEach { selection ->
            val optionId = selection.optionId.toUUID()
            val option = SponsoringOptionEntity.findById(optionId)
                ?: throw NotFoundException("Option $optionId not found")
            val noTranslation = translationEntity
                .listTranslationsByOptionAndLanguage(optionId, partnership.language)
                .isEmpty()
            if (noTranslation) {
                throw ForbiddenException(
                    "Option $optionId doesn't have a translation for language ${partnership.language}",
                )
            }

            // Create partnership option with selection data based on selection type
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.packId = suggestedPack.id
                this.option = option

                // Set selection data based on the polymorphic selection type
                when (selection) {
                    is TextSelection -> {
                        // No additional data needed for text selections
                    }

                    is QuantitativeSelection -> {
                        this.selectedQuantity = selection.selectedQuantity
                    }

                    is NumberSelection -> {
                        // For number selections, the quantity is the fixed quantity from the option
                        this.selectedQuantity = option.fixedQuantity
                    }

                    is SelectableSelection -> {
                        // Validate that the selected value ID exists for this option
                        val selectedValueUUID = selection.selectedValueId.toUUID()
                        val selectedValueEntity = option.selectableValues.find { it.id.value == selectedValueUUID }

                        if (selectedValueEntity == null) {
                            val validValueIds = option.selectableValues.map { "${it.value} (${it.id.value})" }
                            throw ForbiddenException(
                                "Selected value ID '${selection.selectedValueId}' is not valid for option $optionId. " +
                                    "Valid values: ${validValueIds.joinToString(", ")}",
                            )
                        }
                        this.selectedValue = selectedValueEntity
                    }
                }
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
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.suggestionApprovedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = findPartnership(event.id.value, partnershipId)
        partnership.suggestionDeclinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    private fun findPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity =
        partnershipEntity
            .singleByEventAndPartnership(eventId, partnershipId)
            ?: throw NotFoundException("Partnership not found")
}
