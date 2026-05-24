package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.time.Clock

object PartnershipSupportVideosTable : UUIDTable("partnership_support_videos") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val eventId = reference("event_id", EventsTable)
    val url = text("url")
    val status = enumerationByName<PromotionStatus>("status", length = 20)
    val submittedAt = datetime("submitted_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val reviewedAt = datetime("reviewed_at").nullable()
    val reviewedBy = reference("reviewed_by", UsersTable).nullable()
    val declineReason = text("decline_reason").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        uniqueIndex(partnershipId)
        index(isUnique = false, eventId, status)
    }
}
