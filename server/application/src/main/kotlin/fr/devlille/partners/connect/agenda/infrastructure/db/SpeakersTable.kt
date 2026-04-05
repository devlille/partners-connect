package fr.devlille.partners.connect.agenda.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

private const val SOURCE_COLUMN_LENGTH = 50

object SpeakersTable : UUIDTable("speakers") {
    val externalId = varchar("externalId", length = 255).uniqueIndex()
    val name = varchar("name", length = 255)
    val biography = text("biography").nullable()
    val photoUrl = text("photo_url").nullable()
    val jobTitle = varchar("job_title", length = 255).nullable()
    val pronouns = varchar("pronouns", length = 50).nullable()
    val sourceProvider = enumerationByName<IntegrationProvider>("source", SOURCE_COLUMN_LENGTH)
        .default(IntegrationProvider.OPENPLANNER)
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable).nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
