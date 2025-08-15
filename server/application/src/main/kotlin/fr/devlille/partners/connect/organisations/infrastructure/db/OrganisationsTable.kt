package fr.devlille.partners.connect.organisations.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object OrganisationsTable : UUIDTable("organisations") {
    val name = varchar("name", length = 255)
    val slug = varchar("slug", length = 100).uniqueIndex()
    val headOffice = text("head_office")
    val siret = text("siret").nullable()
    val siren = text("siren").nullable()
    val tva = text("tva").nullable()
    val dAndB = text("d_and_b").nullable()
    val nace = text("nace").nullable()
    val naf = text("naf").nullable()
    val duns = text("duns").nullable()
    val iban = text("iban")
    val bic = text("bic")
    val ribUrl = text("rib_url")
    val creationLocation = text("creation_location")
    val createdAt = datetime("created_at")
    val publishedAt = datetime("published_at")

    val representativeUser = reference("representative_user", UsersTable)
    val representativeRole = varchar("representative_role", length = 255)
}
