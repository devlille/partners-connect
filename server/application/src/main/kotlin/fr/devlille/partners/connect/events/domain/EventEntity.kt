package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class EventEntity(
    val id: UUID,
    val name: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val submissionStartTime: LocalDateTime,
    val submissionEndTime: LocalDateTime,
    val address: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val legalName: String?,
    val siret: String?,
    val siren: String?,
    val tva: String?,
    val dAndB: String?,
    val nace: String?,
    val naf: String?,
    val duns: String?,
    val iban: String?,
    val bic: String?,
    val ribUrl: String?
)
