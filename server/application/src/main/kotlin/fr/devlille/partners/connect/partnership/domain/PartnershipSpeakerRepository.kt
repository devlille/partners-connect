package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.agenda.domain.Speaker
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import io.ktor.server.plugins.NotFoundException
import java.util.UUID

/**
 * Repository interface for managing speaker-partnership associations.
 * Follows the specification contract for PartnershipSpeakerRepository.
 */
interface PartnershipSpeakerRepository {
    /**
     * Attaches a speaker to a partnership if partnership is approved
     * @throws NotFoundException if speaker or partnership not found
     * @throws ConflictException if speaker already attached to partnership
     * @throws ForbiddenException if partnership not approved
     */
    fun attachSpeaker(partnershipId: UUID, speakerId: UUID): SpeakerPartnership

    /**
     * Removes speaker from partnership
     * @throws NotFoundException if association not found
     */
    fun detachSpeaker(partnershipId: UUID, speakerId: UUID)

    /**
     * Gets all speakers associated with a partnership
     * @param partnershipId The partnership ID
     * @return List of speakers associated with the partnership
     */
    fun getSpeakersByPartnership(partnershipId: UUID): List<Speaker>
}
