package fr.devlille.partners.connect.internal.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.BadRequestException
import org.koin.ktor.ext.inject

val AuthorizedOrganisationPlugin = createRouteScopedPlugin(name = "AuthorizedOrganisationPlugin") {
    val authRepository by application.inject<AuthRepository>()
    val userRepository by application.inject<UserRepository>()

    onCall { call ->
        val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Missing org slug")
        val token = call.token
        val userInfo = authRepository.getUserInfo(token)
        val canEdit = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
        if (!canEdit) throw UnauthorizedException("You are not allowed to edit this event")
    }
}
