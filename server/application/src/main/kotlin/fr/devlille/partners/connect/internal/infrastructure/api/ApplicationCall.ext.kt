package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.server.application.ApplicationCall
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

val ApplicationCall.token: String
    get() = request.headers["Authorization"]
        ?: sessions.get<UserSession>()?.let { "Bearer ${it.token}" }
        ?: throw UnauthorizedException(
            code = ErrorCode.TOKEN_MISSING,
            message = "Token is missing from session or headers",
            meta = mapOf(
                MetaKeys.HEADER to "Authorization",
            ),
        )
