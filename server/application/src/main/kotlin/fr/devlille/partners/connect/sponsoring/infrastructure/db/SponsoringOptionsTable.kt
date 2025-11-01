@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.sponsoring.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.sponsoring.domain.NumberDescriptor
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.QuantitativeDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object SponsoringOptionsTable : UUIDTable("sponsoring_options") {
    val eventId = reference("event_id", EventsTable)
    val price = integer("price").nullable()
    val optionType = enumerationByName<OptionType>("option_type", length = 50).default(OptionType.TEXT)
    val quantitativeDescriptor = enumerationByName<QuantitativeDescriptor>(
        "quantitative_descriptor",
        length = 50,
    ).nullable()
    val numberDescriptor = enumerationByName<NumberDescriptor>("number_descriptor", length = 50).nullable()
    val selectableDescriptor = enumerationByName<SelectableDescriptor>("selectable_descriptor", length = 50).nullable()
    val fixedQuantity = integer("fixed_quantity").nullable()
}
