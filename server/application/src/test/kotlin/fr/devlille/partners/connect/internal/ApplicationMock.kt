package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.integrations.infrastructure.bindings.integrationModule
import fr.devlille.partners.connect.module
import fr.devlille.partners.connect.sponsoring.infrastructure.bindings.sponsoringModule
import fr.devlille.partners.connect.users.infrastructure.bindings.userModule
import io.ktor.server.application.Application
import java.util.UUID

fun Application.moduleMockedNetwork() {
    module(
        databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        modules = listOf(mockNetworkingEngineModule, authModule, userModule, sponsoringModule, integrationModule),
    )
}
