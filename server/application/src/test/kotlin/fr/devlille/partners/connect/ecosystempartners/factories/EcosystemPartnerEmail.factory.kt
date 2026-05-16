package fr.devlille.partners.connect.ecosystempartners.factories

import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEmailEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import java.util.UUID

fun insertMockedEcosystemPartnerEmail(
    id: UUID = UUID.randomUUID(),
    ecosystemPartnerId: UUID,
    email: String = "$id@mock.test",
): EcosystemPartnerEmailEntity {
    val partner = EcosystemPartnerEntity.findById(ecosystemPartnerId)
        ?: error("Ecosystem partner $ecosystemPartnerId must exist before adding an email")
    return EcosystemPartnerEmailEntity.new(id) {
        this.ecosystemPartner = partner
        this.email = email
    }
}
