package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipAssignmentRepository {
    fun generateAssignment(eventId: UUID, companyId: UUID, partnershipId: UUID): ByteArray

    fun updateAssignmentUrl(eventId: UUID, partnershipId: UUID, assignmentUrl: String): UUID
}
