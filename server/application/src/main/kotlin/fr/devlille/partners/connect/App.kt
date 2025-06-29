package fr.devlille.partners.connect

import fr.devlille.partners.connect.events.infrastructure.api.eventRoutes
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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

fun Application.module() {
    Database.connect(
        url = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver"
    )
    transaction {
        SchemaUtils.create(EventsTable)
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
        modules(eventModule)
    }

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        route("events") {
            eventRoutes()
        }
    }
}
