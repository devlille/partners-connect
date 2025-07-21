package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object PartnershipOptionsTable : UUIDTable("partnership_options") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val optionId = uuid("option_id")
}
