package fr.devlille.partners.connect.events.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.days

@Suppress("LongParameterList")
fun insertMockedFutureEvent(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    slug: String = name,
    address: String = "123 Test St, Test City, TC 12345",
    contactEmail: String = "contact@mail.com",
    contactPhone: String? = null,
    submissionStartTime: LocalDateTime? = null,
    submissionEndTime: LocalDateTime? = null,
    orgId: UUID = UUID.randomUUID(),
): EventEntity {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val yesterday = now.toInstant(TimeZone.UTC).minus(duration = 1.days).toLocalDateTime(TimeZone.UTC)
    val tomorrow = now.toInstant(TimeZone.UTC).plus(duration = 1.days).toLocalDateTime(TimeZone.UTC)
    return EventEntity.new(id) {
        this.name = name
        this.slug = slug
        this.startTime = LocalDateTime.parse("${now.year + 1}-12-01T00:00:00")
        this.endTime = LocalDateTime.parse("${now.year + 1}-12-31T23:59:59")
        this.submissionStartTime = submissionStartTime ?: yesterday
        this.submissionEndTime = submissionEndTime ?: tomorrow
        this.address = address
        this.contactPhone = contactPhone
        this.contactEmail = contactEmail
        this.organisation = OrganisationEntity[orgId]
    }
}

@Suppress("LongParameterList")
fun insertMockedPastEvent(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    slug: String = name,
    address: String = "123 Test St, Test City, TC 12345",
    contactEmail: String = "contact@mail.com",
    contactPhone: String? = null,
    orgId: UUID = UUID.randomUUID(),
): EventEntity {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    return EventEntity.new(id) {
        this.name = name
        this.slug = slug
        this.startTime = LocalDateTime.parse("${now.year - 1}-12-01T00:00:00")
        this.endTime = LocalDateTime.parse("${now.year - 1}-12-31T23:59:59")
        this.submissionStartTime = LocalDateTime.parse("${now.year - 1}-11-01T00:00:00")
        this.submissionEndTime = LocalDateTime.parse("${now.year - 1}-11-30T23:59:59")
        this.address = address
        this.contactPhone = contactPhone
        this.contactEmail = contactEmail
        this.organisation = OrganisationEntity[orgId]
    }
}
