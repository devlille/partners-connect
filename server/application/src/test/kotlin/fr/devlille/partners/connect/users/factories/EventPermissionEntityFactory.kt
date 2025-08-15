package fr.devlille.partners.connect.users.factories

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedOrgaPermission(
    orgId: UUID = UUID.randomUUID(),
    user: UserEntity,
    canEdit: Boolean = true,
): OrganisationPermissionEntity = transaction {
    OrganisationPermissionEntity.new {
        this.organisation = OrganisationEntity[orgId]
        this.user = user
        this.canEdit = canEdit
    }
}
