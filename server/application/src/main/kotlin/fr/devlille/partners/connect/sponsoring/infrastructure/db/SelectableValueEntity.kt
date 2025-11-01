package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

/**
 * Database entity for selectable values in TYPED_SELECTABLE sponsoring options.
 *
 * Each entity represents a single selectable value (e.g., "3x3m", "6x6m")
 * associated with a specific sponsoring option.
 */
class SelectableValueEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SelectableValueEntity>(SelectableValuesTable) {
        /**
         * Create a single selectable value for an option.
         * Used during option creation when organizers provide selectable_values array.
         */
        fun createForOption(optionId: UUID, value: String, price: Int): SelectableValueEntity {
            return new {
                this.option = SponsoringOptionEntity[optionId]
                this.value = value
                this.price = price
            }
        }

        /**
         * Delete all selectable values for a specific option.
         * Used when organizers update or delete TYPED_SELECTABLE options.
         */
        fun deleteAllByOption(optionId: UUID) {
            find { SelectableValuesTable.optionId eq optionId }.forEach { it.delete() }
        }
    }

    var option by SponsoringOptionEntity referencedOn SelectableValuesTable.optionId
    var value by SelectableValuesTable.value
    var price by SelectableValuesTable.price
    var createdAt by SelectableValuesTable.createdAt
}
