package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipSuggestionRepositoryExposed : PartnershipSuggestionRepository {
    override fun suggest(
        eventId: String,
        companyId: String,
        partnershipId: String,
        input: SuggestPartnership,
    ): String = transaction {
        val partnershipUUID = UUID.fromString(partnershipId)
        val partnership = PartnershipEntity.findById(partnershipUUID)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        if (partnership.eventId != UUID.fromString(eventId) || partnership.companyId != UUID.fromString(companyId)) {
            throw BadRequestException("Mismatch between path and partnership")
        }

        val packUUID = UUID.fromString(input.packId)
        val suggestedPack = SponsoringPackEntity.findById(packUUID)
            ?: throw NotFoundException("Pack ${input.packId} not found")

        val optionalOptionIds = PackOptionsTable
            .selectAll()
            .where { (PackOptionsTable.pack eq packUUID) and (PackOptionsTable.required eq false) }
            .map { it[PackOptionsTable.option].value }

        val optionsUUID = input.optionIds.map { UUID.fromString(it) }
        val unknownOptions = optionsUUID.filterNot { it in optionalOptionIds }
        if (unknownOptions.isNotEmpty()) {
            throw BadRequestException("Some options are not optional in the suggested pack: $unknownOptions")
        }

        // Remove previous suggested options
        PartnershipOptionEntity.find { PartnershipOptionsTable.partnershipId eq suggestedPack.id }
            .forEach { it.delete() }

        optionsUUID.forEach {
            SponsoringOptionEntity.findById(it) ?: throw NotFoundException("Option $it not found")
            val hasTranslation = OptionTranslationEntity
                .find {
                    (OptionTranslationsTable.option eq it) and
                        (OptionTranslationsTable.language eq partnership.language)
                }
                .empty().not()

            if (!hasTranslation) {
                throw BadRequestException("Option $it does not have a translation for language ${partnership.language}")
            }
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.optionId = it
            }
        }

        partnership.suggestionPackId = suggestedPack.id.value
        partnership.suggestionSentAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.suggestionApprovedAt = null
        partnership.suggestionDeclinedAt = null

        partnership.id.value.toString()
    }

    override fun approve(eventId: String, companyId: String, partnershipId: String): String = transaction {
        val partnership = findPartnership(eventId, companyId, partnershipId)
        partnership.suggestionApprovedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value.toString()
    }

    override fun decline(eventId: String, companyId: String, partnershipId: String): String = transaction {
        val partnership = findPartnership(eventId, companyId, partnershipId)
        partnership.suggestionDeclinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value.toString()
    }

    private fun findPartnership(eventId: String, companyId: String, partnershipId: String): PartnershipEntity {
        val eventUUID = UUID.fromString(eventId)
        val companyUUID = UUID.fromString(companyId)
        val partnershipUUID = UUID.fromString(partnershipId)

        return PartnershipEntity.find {
            (PartnershipsTable.id eq partnershipUUID) and
                (PartnershipsTable.eventId eq eventUUID) and
                (PartnershipsTable.companyId eq companyUUID)
        }.singleOrNull() ?: throw NotFoundException("Partnership not found")
    }
}
