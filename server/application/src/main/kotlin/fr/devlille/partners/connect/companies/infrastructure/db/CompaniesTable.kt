package fr.devlille.partners.connect.companies.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object CompaniesTable : UUIDTable("companies") {
    val name = text("name")
    val siteUrl = text("site_url")
    val headOffice = text("head_office")
    val siret = text("siret")
    val description = text("description").nullable()
    val logoUrlOriginal = text("logo_url_original").nullable()
    val logoUrl1000 = text("logo_url_1000").nullable()
    val logoUrl500 = text("logo_url_500").nullable()
    val logoUrl250 = text("logo_url_250").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
