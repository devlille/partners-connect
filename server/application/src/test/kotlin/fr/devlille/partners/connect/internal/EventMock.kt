package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntityEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedEvent(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Event",
    startTime: String = "2023-01-01T00:00:00",
    endTime: String = "2023-01-02T00:00:00",
    submissionStartTime: String = "2022-12-01T00:00:00",
    submissionEndTime: String = "2022-12-31T23:59:59",
    address: String = "123 Test St, Test City, TC 12345",
    contactEmail: String = "contact@mail.com",
    contactPhone: String? = null,
    legalEntity: LegalEntityEntity = insertLegalEntity(),
): EventEntity = transaction {
    EventEntity.new(id) {
        this.name = name
        this.startTime = LocalDateTime.parse(startTime)
        this.endTime = LocalDateTime.parse(endTime)
        this.submissionStartTime = LocalDateTime.parse(submissionStartTime)
        this.submissionEndTime = LocalDateTime.parse(submissionEndTime)
        this.address = address
        this.contactPhone = contactPhone
        this.contactEmail = contactEmail
        this.legalEntity = legalEntity
    }
}
