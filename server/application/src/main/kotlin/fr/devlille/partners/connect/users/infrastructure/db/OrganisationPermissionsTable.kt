package fr.devlille.partners.connect.users.infrastructure.db

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object OrganisationPermissionsTable : UUIDTable("organisation_permissions") {
    val organisationId = reference("organisation_id", OrganisationsTable)
    val userId = reference("user_id", UsersTable)
    val canEdit = bool("can_edit").default(true)

    init {
        uniqueIndex(organisationId, userId)
    }
}
