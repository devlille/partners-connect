package fr.devlille.partners.connect.users.infrastructure.db

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class OrganisationPermissionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<OrganisationPermissionEntity>(OrganisationPermissionsTable)

    var organisation by OrganisationEntity referencedOn OrganisationPermissionsTable.organisationId
    var user by UserEntity referencedOn OrganisationPermissionsTable.userId
    var canEdit by OrganisationPermissionsTable.canEdit
}

fun UUIDEntityClass<OrganisationPermissionEntity>.listUserGrantedByOrgId(
    organisationId: UUID,
): SizedIterable<OrganisationPermissionEntity> = this.find {
    (OrganisationPermissionsTable.organisationId eq organisationId) and (OrganisationPermissionsTable.canEdit eq true)
}

fun UUIDEntityClass<OrganisationPermissionEntity>.singleEventPermission(
    organisationId: UUID,
    userId: UUID,
): OrganisationPermissionEntity? = this
    .find {
        (OrganisationPermissionsTable.organisationId eq organisationId) and
            (OrganisationPermissionsTable.userId eq userId)
    }
    .singleOrNull()

fun UUIDEntityClass<OrganisationPermissionEntity>.hasPermission(organisationId: UUID, userId: UUID): Boolean = this
    .find {
        (OrganisationPermissionsTable.organisationId eq organisationId) and
            (OrganisationPermissionsTable.canEdit eq true) and
            (OrganisationPermissionsTable.userId eq userId)
    }
    .empty().not()
