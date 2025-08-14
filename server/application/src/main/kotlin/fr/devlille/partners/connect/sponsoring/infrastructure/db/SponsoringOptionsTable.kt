@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.sponsoring.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object SponsoringOptionsTable : UUIDTable("sponsoring_options") {
    val eventId = reference("event_id", EventsTable)
    val price = integer("price").nullable()
}
