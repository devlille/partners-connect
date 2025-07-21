package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object PartnershipEmailsTable : UUIDTable("company_emails") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val email = text("email")
}
