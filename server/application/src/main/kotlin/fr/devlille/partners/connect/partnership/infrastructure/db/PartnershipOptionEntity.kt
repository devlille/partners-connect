package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.NumberSelection
import fr.devlille.partners.connect.partnership.domain.PartnershipOptionSelection
import fr.devlille.partners.connect.partnership.domain.QuantitativeSelection
import fr.devlille.partners.connect.partnership.domain.SelectableSelection
import fr.devlille.partners.connect.partnership.domain.TextSelection
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SelectableValueEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class PartnershipOptionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipOptionEntity>(PartnershipOptionsTable) {
        fun create(
            selection: PartnershipOptionSelection,
            partnership: PartnershipEntity,
            pack: SponsoringPackEntity,
            option: SponsoringOptionEntity,
        ) {
            new {
                this.partnership = partnership
                this.pack = pack
                this.option = option

                // Set selection data based on the polymorphic selection type
                when (selection) {
                    is TextSelection -> {
                        // No additional data needed for text selections
                    }

                    is QuantitativeSelection -> {
                        this.selectedQuantity = selection.selectedQuantity
                    }

                    is NumberSelection -> {
                        // For number selections, the quantity is the fixed quantity from the option
                        this.selectedQuantity = option.fixedQuantity
                    }

                    is SelectableSelection -> {
                        // Validate that the selected value ID exists for this option
                        val selectedValueUUID = selection.selectedValueId.toUUID()
                        val selectedValue = option.selectableValues.find { it.id.value == selectedValueUUID }
                        if (selectedValue == null) {
                            val validValueIds = option.selectableValues.map { "${it.value} (${it.id.value})" }
                            val message = """
                            Selected value ID '${selection.selectedValueId}' is not valid for option ${option.id}.
                            Valid values: ${validValueIds.joinToString(", ")}
                            """.trimIndent()
                            throw ForbiddenException(message)
                        }
                        this.selectedValue = selectedValue
                    }
                }
            }
        }

        fun listByPartnershipAndPack(partnershipId: UUID, packId: UUID): SizedIterable<PartnershipOptionEntity> = this
            .find {
                PartnershipOptionsTable.partnershipId eq partnershipId and (PartnershipOptionsTable.packId eq packId)
            }

        fun deleteAllByPartnershipId(partnershipId: UUID, packId: UUID): Unit = this
            .find {
                (PartnershipOptionsTable.partnershipId eq partnershipId) and (PartnershipOptionsTable.packId eq packId)
            }
            .forEach { it.delete() }
    }

    var partnership by PartnershipEntity referencedOn PartnershipOptionsTable.partnershipId
    var pack by SponsoringPackEntity referencedOn PartnershipOptionsTable.packId
    var option by SponsoringOptionEntity referencedOn PartnershipOptionsTable.optionId
    var selectedQuantity by PartnershipOptionsTable.selectedQuantity
    var selectedValue by SelectableValueEntity optionalReferencedOn PartnershipOptionsTable.selectedValueId
}
