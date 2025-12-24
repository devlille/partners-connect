package fr.devlille.partners.connect.events.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedEvent(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Event",
    slug: String? = null,
    startTime: String = "2023-01-01T00:00:00",
    endTime: String = "2023-01-02T00:00:00",
    submissionStartTime: String = "2022-12-01T00:00:00",
    submissionEndTime: String = "2022-12-31T23:59:59",
    address: String = "123 Test St, Test City, TC 12345",
    contactEmail: String = "contact@mail.com",
    contactPhone: String? = null,
    orgId: UUID = UUID.randomUUID(),
): EventEntity = transaction {
    EventEntity.new(id) {
        this.name = name
        this.slug = slug ?: "${name.slugify()}-${id.toString().take(8)}"
        this.startTime = LocalDateTime.parse(startTime)
        this.endTime = LocalDateTime.parse(endTime)
        this.submissionStartTime = LocalDateTime.parse(submissionStartTime)
        this.submissionEndTime = LocalDateTime.parse(submissionEndTime)
        this.address = address
        this.contactPhone = contactPhone
        this.contactEmail = contactEmail
        this.organisation = OrganisationEntity[orgId]
    }
}

@Suppress("LongParameterList")
fun insertMockedFutureEvent(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    slug: String = name,
    address: String = "123 Test St, Test City, TC 12345",
    contactEmail: String = "contact@mail.com",
    contactPhone: String? = null,
    orgId: UUID = UUID.randomUUID(),
): EventEntity = transaction {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    EventEntity.new(id) {
        this.name = name
        this.slug = slug
        this.startTime = LocalDateTime.parse("${now.year + 1}-12-01T00:00:00")
        this.endTime = LocalDateTime.parse("${now.year + 1}-12-31T23:59:59")
        this.submissionStartTime = LocalDateTime.parse("${now.year + 1}-11-01T00:00:00")
        this.submissionEndTime = LocalDateTime.parse("${now.year + 1}-11-30T23:59:59")
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
): EventEntity = transaction {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    EventEntity.new(id) {
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

@Suppress("LongParameterList")
fun insertMockedEventWithOrga(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Event",
    slug: String? = null,
    startTime: String = "2023-01-01T00:00:00",
    endTime: String = "2023-01-02T00:00:00",
    submissionStartTime: String = "2022-12-01T00:00:00",
    submissionEndTime: String = "2022-12-31T23:59:59",
    address: String = "123 Test St, Test City, TC 12345",
    contactEmail: String = "contact@mail.com",
    contactPhone: String? = null,
    organisation: OrganisationEntity = insertMockedOrganisationEntity(),
): EventEntity = transaction {
    EventEntity.new(id) {
        this.name = name
        this.slug = slug ?: "${name.slugify()}-${id.toString().take(8)}"
        this.startTime = LocalDateTime.parse(startTime)
        this.endTime = LocalDateTime.parse(endTime)
        this.submissionStartTime = LocalDateTime.parse(submissionStartTime)
        this.submissionEndTime = LocalDateTime.parse(submissionEndTime)
        this.address = address
        this.contactPhone = contactPhone
        this.contactEmail = contactEmail
        this.organisation = organisation
    }
}
