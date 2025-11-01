@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.sponsoring.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Database table for storing selectable values associated with TYPED_SELECTABLE sponsoring options.
 * Each selectable option can have multiple predefined values that partners can choose from.
 */
object SelectableValuesTable : UUIDTable("selectable_values") {
    val optionId = reference("option_id", SponsoringOptionsTable)
    val value = varchar("value", 255)
    val price = integer("price")
    val createdAt = datetime("created_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }

    init {
        // Unique constraint to prevent duplicate values per option
        index(true, optionId, value)
    }
}
