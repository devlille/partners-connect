package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.agenda.domain.Speaker
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.domain.PartnershipSpeakerRepository
import fr.devlille.partners.connect.partnership.domain.SpeakerPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

/**
 * Exposed-based implementation of PartnershipSpeakerRepository.
 * Handles speaker-partnership associations with proper validation.
 */
class PartnershipSpeakerRepositoryExposed : PartnershipSpeakerRepository {
    override fun attachSpeaker(partnershipId: UUID, speakerId: UUID): SpeakerPartnership = transaction {
        val partnership = PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership not found")

        if (partnership.validatedAt == null) {
            throw ForbiddenException("Can only attach speakers to validated partnerships")
        }

        val speaker = SpeakerEntity.findById(speakerId)
            ?: throw NotFoundException("Speaker not found")
        if (speaker.event.id != partnership.event.id) {
            throw BadRequestException("Speaker and partnership must belong to the same event")
        }

        // Check if association already exists
        val existing = SpeakerPartnershipEntity.find {
            (SpeakerPartnershipTable.speakerId eq speakerId) and
                (SpeakerPartnershipTable.partnershipId eq partnershipId)
        }.singleOrNull()

        if (existing != null) {
            throw ConflictException("Speaker is already attached to this partnership")
        }

        // Create new association
        val association = SpeakerPartnershipEntity.new {
            this.speaker = speaker
            this.partnership = partnership
            this.createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        }
        SpeakerPartnership(
            id = association.id.value.toString(),
            speakerId = speakerId.toString(),
            partnershipId = partnershipId.toString(),
            createdAt = association.createdAt,
        )
    }

    override fun detachSpeaker(partnershipId: UUID, speakerId: UUID) = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership not found")
        SpeakerEntity.findById(speakerId)
            ?: throw NotFoundException("Speaker not found")

        // Find and delete association
        val association = SpeakerPartnershipEntity.find {
            (SpeakerPartnershipTable.speakerId eq speakerId) and
                (SpeakerPartnershipTable.partnershipId eq partnershipId)
        }.singleOrNull() ?: throw NotFoundException("Speaker is not attached to this partnership")

        association.delete()
    }

    override fun getSpeakersByPartnership(partnershipId: UUID): List<Speaker> = transaction {
        SpeakerPartnershipEntity.find {
            SpeakerPartnershipTable.partnershipId eq partnershipId
        }.map { association ->
            val speaker = association.speaker
            Speaker(
                id = speaker.id.value.toString(),
                name = speaker.name,
                biography = speaker.biography,
                jobTitle = speaker.jobTitle,
                photoUrl = speaker.photoUrl,
                pronouns = speaker.pronouns,
            )
        }.sortedBy { it.name }
    }
}
