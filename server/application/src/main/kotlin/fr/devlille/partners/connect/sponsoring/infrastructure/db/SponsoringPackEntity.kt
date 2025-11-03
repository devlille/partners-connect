package fr.devlille.partners.connect.sponsoring.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.sponsoring.domain.NumberDescriptor
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class SponsoringPackEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SponsoringPackEntity>(SponsoringPacksTable)

    var event by EventEntity referencedOn SponsoringPacksTable.eventId
    var name by SponsoringPacksTable.name
    var basePrice by SponsoringPacksTable.basePrice
    var maxQuantity by SponsoringPacksTable.maxQuantity
    val options by SponsoringOptionEntity via PackOptionsTable
}

fun UUIDEntityClass<SponsoringPackEntity>.listPacksByEvent(eventId: UUID): List<SponsoringPackEntity> = this
    .find { SponsoringPacksTable.eventId eq eventId }
    .toList()

fun UUIDEntityClass<SponsoringPackEntity>.singlePackById(eventId: UUID, packId: UUID): SponsoringPackEntity = this
    .find { (SponsoringPacksTable.id eq packId) and (SponsoringPacksTable.eventId eq eventId) }
    .singleOrNull()
    ?: throw NotFoundException("Pack not found")

/**
 * Determines if this pack includes booth options based on attached sponsoring options.
 *
 * Replaces the deprecated `withBooth` field by checking if the pack contains
 * any options with type_descriptor = "booth" (SelectableDescriptor.BOOTH).
 *
 * For backward compatibility, falls back to the legacy `withBooth` field if no
 * booth-related options are found.
 *
 * @return true if the pack has at least one booth-related option or legacy withBooth is true
 */
fun SponsoringPackEntity.hasBoothFromOptions(): Boolean = options.any { option ->
    option.optionType == OptionType.TYPED_SELECTABLE &&
        option.selectableDescriptor == SelectableDescriptor.BOOTH
}

/**
 * Calculates the total number of tickets available in this pack based on attached sponsoring options.
 *
 * Replaces the deprecated `nbTickets` field by summing up the fixed_quantity
 * from all options with type_descriptor = "nb_ticket" (NumberDescriptor.NB_TICKET).
 *
 * For backward compatibility, falls back to the legacy `nbTickets` field if no
 * ticket-related options are found.
 *
 * @return total ticket count from all ticket-related options in the pack, or legacy nbTickets if no options found
 */
fun SponsoringPackEntity.getTotalTicketsFromOptions(): Int = options.filter { option ->
    option.optionType == OptionType.TYPED_NUMBER && option.numberDescriptor == NumberDescriptor.NB_TICKET
}.sumOf { option -> option.fixedQuantity ?: 0 }
