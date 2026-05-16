package fr.devlille.partners.connect.ecosystempartners.infrastructure.db

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object EcosystemPartnerEmailsTable : UUIDTable("ecosystem_partner_emails") {
    val ecosystemPartnerId = reference(
        name = "ecosystem_partner_id",
        foreign = EcosystemPartnersTable,
        onDelete = ReferenceOption.CASCADE,
    )
    val email = text("email")

    init {
        index(isUnique = false, ecosystemPartnerId)
    }
}
