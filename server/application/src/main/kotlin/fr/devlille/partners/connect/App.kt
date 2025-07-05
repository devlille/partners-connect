package fr.devlille.partners.connect

import fr.devlille.partners.connect.internal.infrastructure.api.UserSession
import fr.devlille.partners.connect.auth.infrastructure.api.authRoutes
import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.auth.infrastructure.plugins.configureSecurity
import fr.devlille.partners.connect.events.infrastructure.api.eventRoutes
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkEngineModule
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.users.infrastructure.api.userRoutes
import fr.devlille.partners.connect.users.infrastructure.bindings.userModule
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

const val PORT = 8080

fun main() {
    embeddedServer(
        factory = Netty,
        port = PORT,
        module = Application::module
    ).start(wait = true)
}

fun Application.module(
    databaseUrl: String = SystemVarEnv.Exposed.dbUrl,
    databaseDriver: String = SystemVarEnv.Exposed.dbDriver,
    databaseUser: String = SystemVarEnv.Exposed.dbUser,
    databasePassword: String = SystemVarEnv.Exposed.dbPassword,
    modules: List<Module> = listOf(networkEngineModule, authModule, eventModule, userModule)
) {
    Database.connect(
        url = databaseUrl,
        driver = databaseDriver,
        user = databaseUser,
        password = databasePassword
    )
    transaction {
        SchemaUtils.create(EventsTable, UsersTable, EventPermissionsTable)
    }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
    }

    install(Koin) {
        slf4jLogger()
        modules(modules)
    }

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respondText(text = cause.message ?: "400 Bad Request", status = HttpStatusCode.BadRequest)
        }
        exception<UnauthorizedException> { call, cause ->
            call.respondText(text = cause.message ?: "401 Unauthorized", status = HttpStatusCode.Unauthorized)
        }
        exception<NotFoundException> { call, cause ->
            call.respondText(text = cause.message ?: "404 Not Found", status = HttpStatusCode.NotFound)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    install(Sessions) {
        cookie<UserSession>("user_session")
    }

    val redirects = mutableMapOf<String, String>()
    configureSecurity { state, redirectUrl ->
        redirects[state] = redirectUrl
    }

    routing {
        route("auth") {
            authRoutes { redirects[it] }
        }
        route("events") {
            eventRoutes()
        }
        userRoutes()
    }
}
