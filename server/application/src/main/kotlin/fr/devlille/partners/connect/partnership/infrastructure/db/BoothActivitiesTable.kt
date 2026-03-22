package fr.devlille.partners.connect.partnership.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object BoothActivitiesTable : UUIDTable("booth_activities") {
    val partnershipId = reference("partnership_id", PartnershipsTable, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", length = 255)
    val description = text("description")
    val startTime = datetime("start_time").nullable()
    val endTime = datetime("end_time").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
