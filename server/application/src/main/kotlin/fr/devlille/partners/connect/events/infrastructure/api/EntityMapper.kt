package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventEntity
import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun CreateOrUpdateEventRequest.toEntity(id: UUID): EventEntity = EventEntity(
    id = id,
    name = name,
    startTime = LocalDateTime.parse(start_time),
    endTime = LocalDateTime.parse(end_time),
    submissionStartTime = LocalDateTime.parse(submission_start_time),
    submissionEndTime = LocalDateTime.parse(submission_end_time),
    address = address,
    contactPhone = contact.phone,
    contactEmail = contact.email,
    legalName = legal?.name,
    siret = legal?.siret,
    siren = legal?.siren,
    tva = legal?.tva,
    dAndB = legal?.d_and_b,
    nace = legal?.nace,
    naf = legal?.naf,
    duns = legal?.duns,
    iban = banking?.iban,
    bic = banking?.bic,
    ribUrl = banking?.rib_url
)
