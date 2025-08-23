package fr.devlille.partners.connect.internal.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.server.application.createRouteScopedPlugin
import org.koin.ktor.ext.inject

val AuthorizedOrganisationPlugin = createRouteScopedPlugin(name = "AuthorizedOrganisationPlugin") {
    val authRepository by application.inject<AuthRepository>()
    val userRepository by application.inject<UserRepository>()

    onCall { call ->
        val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException(
            code = ErrorCode.MISSING_REQUIRED_PARAMETER,
            message = "Missing org slug",
            meta = mapOf(MetaKeys.FIELD to "orgSlug"),
        )
        val token = call.token
        val userInfo = authRepository.getUserInfo(token)
        val canEdit = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
        if (!canEdit) {
            throw UnauthorizedException(
                code = ErrorCode.NO_EDIT_PERMISSION,
                message = "You are not allowed to edit this event",
                meta = mapOf(
                    MetaKeys.EMAIL to userInfo.email,
                    MetaKeys.ORGANISATION to orgSlug,
                    MetaKeys.REQUIRED_ROLE to "organizer",
                ),
            )
        }
    }
}
