@file:Suppress("MagicNumber")
package fr.devlille.partners.connect.events.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object EventsTable : UUIDTable("events") {
    val name: Column<String> = varchar("name", 255)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val submissionStartTime = datetime("submission_start_time")
    val submissionEndTime = datetime("submission_end_time")
    val address = text("address").nullable()

    val contactPhone = varchar("contact_phone", 30).nullable()
    val contactEmail = varchar("contact_email", 255).nullable()

    val legalName = varchar("legal_name", 255).nullable()
    val siret = varchar("siret", 20).nullable()
    val siren = varchar("siren", 20).nullable()
    val tva = varchar("tva", 50).nullable()
    val dAndB = varchar("d_and_b", 50).nullable()
    val nace = varchar("nace", 50).nullable()
    val naf = varchar("naf", 50).nullable()
    val duns = varchar("duns", 50).nullable()

    val iban = varchar("iban", 50).nullable()
    val bic = varchar("bic", 20).nullable()
    val ribUrl = text("rib_url").nullable()

    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
