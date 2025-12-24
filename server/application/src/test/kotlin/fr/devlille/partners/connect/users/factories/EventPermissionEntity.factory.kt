package fr.devlille.partners.connect.users.factories

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import java.util.UUID

fun insertMockedOrgaPermission(
    orgId: UUID,
    userId: UUID,
    canEdit: Boolean = true,
): OrganisationPermissionEntity = OrganisationPermissionEntity.new {
    this.organisation = OrganisationEntity[orgId]
    this.user = UserEntity[userId]
    this.canEdit = canEdit
}
