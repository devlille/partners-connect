package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object PartnershipOptionsTable : UUIDTable("partnership_options") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val packId = reference("pack_id", SponsoringPacksTable)
    val optionId = reference("option_id", SponsoringOptionsTable)
}
