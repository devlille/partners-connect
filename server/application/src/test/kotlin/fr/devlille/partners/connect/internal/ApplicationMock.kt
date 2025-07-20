package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.companies.infrastructure.bindings.companyModule
import fr.devlille.partners.connect.integrations.infrastructure.bindings.integrationModule
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.module
import fr.devlille.partners.connect.sponsoring.infrastructure.bindings.sponsoringModule
import fr.devlille.partners.connect.users.infrastructure.bindings.userModule
import io.ktor.client.engine.HttpClientEngine
import io.ktor.server.application.Application
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.UUID

fun Application.moduleMocked(
    mockNetwork: Module = module {
        single<HttpClientEngine> { mockEngine }
    },
    mockStorage: Module = module {
        single<Storage> { mockk() }
    },
) {
    module(
        databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        modules = listOf(
            authModule,
            userModule,
            sponsoringModule,
            companyModule,
            integrationModule,
            mockNetwork,
            mockStorage,
        ),
    )
}
