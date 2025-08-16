package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import java.util.UUID

interface PartnershipRepository {
    fun register(eventId: UUID, register: RegisterPartnership): UUID

    fun getById(eventId: UUID, partnershipId: UUID): Partnership

    fun getCompanyByPartnershipId(eventId: UUID, partnershipId: UUID): Company

    fun validate(eventId: UUID, partnershipId: UUID): UUID

    fun decline(eventId: UUID, partnershipId: UUID): UUID

    fun listByCompany(companyId: UUID): List<PartnershipItem>
}
