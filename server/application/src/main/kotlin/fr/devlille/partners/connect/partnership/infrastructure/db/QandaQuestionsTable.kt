package fr.devlille.partners.connect.partnership.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object QandaQuestionsTable : UUIDTable("qanda_questions") {
    val partnershipId = reference("partnership_id", PartnershipsTable, onDelete = ReferenceOption.CASCADE)
    val question = text("question")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
