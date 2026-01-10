package fr.devlille.partners.connect.internal.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import fr.devlille.partners.connect.users.domain.UserOrganisationPermission
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import org.koin.ktor.ext.inject

val AuthorizedOrganisationPlugin = createRouteScopedPlugin(name = "AuthorizedOrganisationPlugin") {
    val authRepository by application.inject<AuthRepository>()
    val userRepository by application.inject<UserRepository>()

    onCall { call ->
        val token = call.token
        val orgSlug = call.parameters.orgSlug
        val userInfo = authRepository.getUserInfo(token)
        val userPerm = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
        if (!userPerm.canEdit) throw UnauthorizedException("You are not allowed to edit this event")
        call.attributes.put(AuthorizedOrganisationPluginKeys.UserKey, userPerm)
    }
}

private object AuthorizedOrganisationPluginKeys {
    val UserKey = AttributeKey<UserOrganisationPermission>("AuthorizedOrganisationUser")
}

val Attributes.user: UserOrganisationPermission
    get() = this[AuthorizedOrganisationPluginKeys.UserKey]
