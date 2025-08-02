@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntitiesTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object EventsTable : UUIDTable("events") {
    val name: Column<String> = varchar("name", 255)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val submissionStartTime = datetime("submission_start_time")
    val submissionEndTime = datetime("submission_end_time")
    val address = text("address")

    val contactEmail = varchar("contact_email", 255)
    val contactPhone = varchar("contact_phone", 30).nullable()
    val legalEntityId = reference("legal_entity_id", LegalEntitiesTable)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
