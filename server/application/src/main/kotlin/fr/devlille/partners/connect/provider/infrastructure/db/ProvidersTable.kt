@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.provider.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object ProvidersTable : UUIDTable("providers") {
    val name = varchar("name", 255)
    val type = varchar("type", 100)
    val website = text("website").nullable()
    val phone = varchar("phone", 30).nullable()
    val email = varchar("email", 255).nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
