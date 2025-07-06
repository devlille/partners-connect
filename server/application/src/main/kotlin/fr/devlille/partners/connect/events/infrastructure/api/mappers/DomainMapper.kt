package fr.devlille.partners.connect.events.infrastructure.api.mappers

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.infrastructure.api.CreateOrUpdateEventRequest
import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun CreateOrUpdateEventRequest.toDomain(id: UUID = UUID.randomUUID()): Event = Event(
    id = id,
    name = name,
    startTime = LocalDateTime.parse(startTime),
    endTime = LocalDateTime.parse(endTime),
    submissionStartTime = LocalDateTime.parse(submissionStartTime),
    submissionEndTime = LocalDateTime.parse(submissionEndTime),
    address = address,
    contactPhone = contact.phone,
    contactEmail = contact.email,
    legalName = legal?.name,
    siret = legal?.siret,
    siren = legal?.siren,
    tva = legal?.tva,
    dAndB = legal?.dAndB,
    nace = legal?.nace,
    naf = legal?.naf,
    duns = legal?.duns,
    iban = banking?.iban,
    bic = banking?.bic,
    ribUrl = banking?.ribUrl
)
