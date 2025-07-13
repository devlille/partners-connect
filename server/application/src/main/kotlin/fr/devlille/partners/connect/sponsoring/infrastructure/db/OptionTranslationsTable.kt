package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object OptionTranslationsTable : UUIDTable("option_translations") {
    val option = reference("option_id", SponsoringOptionsTable)
    val language = text("language")
    val name = text("name")
    val description = text("description").nullable()

    init {
        uniqueIndex(option, language)
    }
}
