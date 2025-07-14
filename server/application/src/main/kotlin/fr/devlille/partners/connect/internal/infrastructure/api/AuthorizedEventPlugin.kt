package fr.devlille.partners.connect.internal.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.BadRequestException
import org.koin.ktor.ext.inject

val AuthorizedEventPlugin = createRouteScopedPlugin(name = "AuthorizedEventPlugin") {
    val authRepository by application.inject<AuthRepository>()
    val userRepository by application.inject<UserRepository>()

    onCall { call ->
        val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing eventId")
        val token = call.token
        val userInfo = authRepository.getUserInfo(token)
        val canEdit = userRepository.hasEditPermissionByEmail(userInfo.email, eventId)
        if (!canEdit) throw UnauthorizedException("You are not allowed to edit this event")
    }
}
