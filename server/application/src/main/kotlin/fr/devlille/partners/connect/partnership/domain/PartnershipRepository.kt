package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipRepository {
    fun register(eventId: UUID, companyId: UUID, register: RegisterPartnership): UUID

    fun getById(eventId: UUID, partnershipId: UUID): Partnership

    fun getByCompany(eventId: UUID, companyId: UUID): Partnership?

    fun validate(eventId: UUID, partnershipId: UUID): UUID

    fun decline(eventId: UUID, partnershipId: UUID): UUID
}
