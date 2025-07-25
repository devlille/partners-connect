package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
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
    legalName: String? = null,
    siret: String? = null,
    siren: String? = null,
    tva: String? = null,
    dAndB: String? = null,
    nace: String? = null,
    naf: String? = null,
    duns: String? = null,
    iban: String? = null,
    bic: String? = null,
    ribUrl: String? = null,
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
        this.legalName = legalName
        this.siret = siret
        this.siren = siren
        this.tva = tva
        this.dAndB = dAndB
        this.nace = nace
        this.naf = naf
        this.duns = duns
        this.iban = iban
        this.bic = bic
        this.ribUrl = ribUrl
    }
}
