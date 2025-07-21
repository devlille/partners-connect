package fr.devlille.partners.connect

import fr.devlille.partners.connect.auth.infrastructure.api.authRoutes
import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.auth.infrastructure.plugins.configureSecurity
import fr.devlille.partners.connect.companies.infrastructure.api.companyRoutes
import fr.devlille.partners.connect.companies.infrastructure.bindings.companyModule
import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialsTable
import fr.devlille.partners.connect.events.infrastructure.api.eventRoutes
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.infrastructure.api.integrationRoutes
import fr.devlille.partners.connect.integrations.infrastructure.bindings.integrationModule
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.UserSession
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkEngineModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.storageModule
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipRoutes
import fr.devlille.partners.connect.partnership.infrastructure.bindings.partnershipModule
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.api.sponsoringRoutes
import fr.devlille.partners.connect.sponsoring.infrastructure.bindings.sponsoringModule
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
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
        module = Application::module,
    ).start(wait = true)
}

fun Application.module(
    databaseUrl: String = SystemVarEnv.Exposed.dbUrl,
    databaseDriver: String = SystemVarEnv.Exposed.dbDriver,
    databaseUser: String = SystemVarEnv.Exposed.dbUser,
    databasePassword: String = SystemVarEnv.Exposed.dbPassword,
    modules: List<Module> = listOf(
        networkEngineModule,
        storageModule,
        authModule,
        eventModule,
        userModule,
        sponsoringModule,
        companyModule,
        partnershipModule,
        integrationModule,
    ),
) {
    configureDatabase(
        url = databaseUrl,
        driver = databaseDriver,
        user = databaseUser,
        password = databasePassword,
    )
    configureCors()
    install(Koin) {
        slf4jLogger()
        modules(modules)
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
        route("auth") {
            authRoutes { redirects[it] }
        }
        route("events") {
            eventRoutes()
        }
        userRoutes()
        sponsoringRoutes()
        companyRoutes()
        partnershipRoutes()
        integrationRoutes()
    }
}

private fun configureDatabase(url: String, driver: String, user: String, password: String) {
    val db = Database.connect(
        url = url,
        driver = driver,
        user = user,
        password = password,
    )
    transaction(db) {
        SchemaUtils.create(
            // integrations
            IntegrationsTable,
            SlackIntegrationsTable,
            // events
            EventsTable,
            // users
            UsersTable,
            EventPermissionsTable,
            // sponsoring
            PackOptionsTable,
            SponsoringOptionsTable,
            SponsoringPacksTable,
            OptionTranslationsTable,
            // companies
            CompaniesTable,
            CompanySocialsTable,
            // partnerships
            PartnershipsTable,
            PartnershipOptionsTable,
            PartnershipEmailsTable,
        )
    }
}

private fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
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
}
