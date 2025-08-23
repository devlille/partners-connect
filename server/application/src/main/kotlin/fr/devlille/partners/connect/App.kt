package fr.devlille.partners.connect

import fr.devlille.partners.connect.auth.infrastructure.api.authRoutes
import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.auth.infrastructure.plugins.configureSecurity
import fr.devlille.partners.connect.billing.infrastructure.bindings.billingModule
import fr.devlille.partners.connect.companies.infrastructure.api.companyRoutes
import fr.devlille.partners.connect.companies.infrastructure.bindings.companyModule
import fr.devlille.partners.connect.events.infrastructure.api.eventBoothPlanRoutes
import fr.devlille.partners.connect.events.infrastructure.api.eventExternalLinkRoutes
import fr.devlille.partners.connect.events.infrastructure.api.eventProviderRoutes
import fr.devlille.partners.connect.events.infrastructure.api.eventRoutes
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.integrations.infrastructure.api.integrationRoutes
import fr.devlille.partners.connect.integrations.infrastructure.bindings.integrationModule
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorResponse
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKey
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.api.UserSession
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkEngineModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.storageModule
import fr.devlille.partners.connect.internal.infrastructure.migrations.MigrationRegistry
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.notifications.infrastructure.bindings.notificationModule
import fr.devlille.partners.connect.organisations.infrastructure.api.organisationRoutes
import fr.devlille.partners.connect.organisations.infrastructure.bindings.organisationModule
import fr.devlille.partners.connect.partnership.infrastructure.api.eventCommunicationPlanRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipAgreementRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipBillingRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipBoothLocationRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipCommunicationRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipSuggestionRoutes
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipTicketingRoutes
import fr.devlille.partners.connect.partnership.infrastructure.bindings.partnershipModule
import fr.devlille.partners.connect.provider.infrastructure.api.providerRoutes
import fr.devlille.partners.connect.provider.infrastructure.bindings.providerModule
import fr.devlille.partners.connect.sponsoring.infrastructure.api.sponsoringRoutes
import fr.devlille.partners.connect.sponsoring.infrastructure.bindings.sponsoringModule
import fr.devlille.partners.connect.tickets.infrastructure.bindings.ticketingModule
import fr.devlille.partners.connect.users.infrastructure.api.userRoutes
import fr.devlille.partners.connect.users.infrastructure.bindings.userModule
import fr.devlille.partners.connect.webhooks.infrastructure.bindings.webhookModule
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

const val PORT = 8080

fun main() {
    embeddedServer(
        factory = Netty,
        port = PORT,
        module = Application::module,
    ).start(wait = true)
}

data class ApplicationConfig(
    val databaseUrl: String = SystemVarEnv.Exposed.dbUrl,
    val databaseDriver: String = SystemVarEnv.Exposed.dbDriver,
    val databaseUser: String = SystemVarEnv.Exposed.dbUser,
    val databasePassword: String = SystemVarEnv.Exposed.dbPassword,
    val enableOpenAPI: Boolean = true,
    val modules: List<Module> = listOf(
        networkEngineModule,
        networkClientModule,
        storageModule,
        authModule,
        organisationModule,
        eventModule,
        userModule,
        sponsoringModule,
        companyModule,
        partnershipModule,
        notificationModule,
        billingModule,
        ticketingModule,
        integrationModule,
        providerModule,
        webhookModule,
    ),
)

fun Application.module(config: ApplicationConfig = ApplicationConfig()) {
    configureDatabase(
        url = config.databaseUrl,
        driver = config.databaseDriver,
        user = config.databaseUser,
        password = config.databasePassword,
    )
    configureCors()
    install(Koin) {
        slf4jLogger()
        modules(config.modules)
    }
    configureContentNegotiation()
    configureStatusPage()
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
    val redirects = mutableMapOf<String, String>()
    configureSecurity { state, redirectUrl ->
        redirects[state] = redirectUrl
    }
    routing {
        if (config.enableOpenAPI) {
            openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
            swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        }
        route("auth") {
            authRoutes { redirects[it] }
        }
        organisationRoutes()
        eventRoutes()
        eventBoothPlanRoutes()
        eventProviderRoutes()
        eventExternalLinkRoutes()
        userRoutes()
        sponsoringRoutes()
        companyRoutes()
        eventCommunicationPlanRoutes()
        partnershipAgreementRoutes()
        partnershipBillingRoutes()
        partnershipBoothLocationRoutes()
        partnershipCommunicationRoutes()
        partnershipRoutes()
        partnershipSuggestionRoutes()
        partnershipTicketingRoutes()
        integrationRoutes()
        providerRoutes()
    }
}

private fun configureDatabase(url: String, driver: String, user: String, password: String) {
    val db = Database.connect(
        url = url,
        driver = driver,
        user = user,
        password = password,
    )

    // Apply all database migrations
    val migrationManager = MigrationRegistry.createManager()
    migrationManager.migrate(db)
}

private fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AcceptLanguage)

        // Allow localhost for development
        allowHost("localhost:3000")
        allowHost("localhost:8080")
        allowHost("127.0.0.1:3000")
        allowHost("127.0.0.1:8080")
        allowHost(
            "localhost:3000",
            schemes = listOf("http", "https"),
        )
        allowHost(
            "localhost:8080",
            schemes = listOf("http", "https"),
        )
    }
}

private fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            },
        )
    }
}

private fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respondWithStructuredError(cause.code, cause.message, cause.status, cause.meta.toStringMap())
        }
        exception<UnauthorizedException> { call, cause ->
            call.respondWithStructuredError(cause.code, cause.message, cause.status, cause.meta.toStringMap())
        }
        exception<ForbiddenException> { call, cause ->
            call.respondWithStructuredError(cause.code, cause.message, cause.status, cause.meta.toStringMap())
        }
        exception<NotFoundException> { call, cause ->
            call.respondWithStructuredError(cause.code, cause.message, cause.status, cause.meta.toStringMap())
        }
        exception<UnsupportedMediaTypeException> { call, cause ->
            call.respondWithStructuredError(cause.code, cause.message, cause.status, cause.meta.toStringMap())
        }
        exception<ConflictException> { call, cause ->
            call.respondWithStructuredError(cause.code, cause.message, cause.status, cause.meta.toStringMap())
        }
        exception<Throwable> { call, _ ->
            call.respondWithStructuredError(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Internal server error",
                HttpStatusCode.InternalServerError,
                emptyMap(),
            )
        }
    }
}

private suspend fun ApplicationCall.respondWithStructuredError(
    code: ErrorCode,
    message: String,
    status: HttpStatusCode,
    meta: Map<String, String>,
) {
    val errorResponse = ErrorResponse(
        code = code.name,
        message = message,
        status = status.value,
        meta = meta,
    )
    respond(status, errorResponse)
}

/**
 * Extension function to convert Map<MetaKey, String> to Map<String, String> for JSON serialization.
 */
private fun Map<MetaKey, String>.toStringMap(): Map<String, String> =
    this.mapKeys { it.key.key }
