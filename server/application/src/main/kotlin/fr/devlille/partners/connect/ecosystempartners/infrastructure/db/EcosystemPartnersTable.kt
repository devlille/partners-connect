package fr.devlille.partners.connect.ecosystempartners.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object EcosystemPartnersTable : UUIDTable("ecosystem_partners") {
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    val categoryId = reference("category_id", EcosystemPartnerCategoriesTable)
    val displayOrder = integer("display_order").nullable()
    val language = text("language").default("en")
    val validatedAt = datetime("validated_at").nullable()
    val declinedAt = datetime("declined_at").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        uniqueIndex(
            "uk_ecosystem_partner_event_company_category",
            eventId,
            companyId,
            categoryId,
        )
        index(isUnique = false, eventId, validatedAt)
    }
}
