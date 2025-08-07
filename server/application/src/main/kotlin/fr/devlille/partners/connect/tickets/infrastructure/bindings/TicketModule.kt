package fr.devlille.partners.connect.tickets.infrastructure.bindings

import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.tickets.application.TicketRepositoryExposed
import fr.devlille.partners.connect.tickets.domain.TicketGateway
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import fr.devlille.partners.connect.tickets.infrastructure.gateways.BilletWebTicketGateway
import org.koin.dsl.module

val ticketingIntegrationsModule = module {
    includes(networkClientModule)

    single<List<TicketGateway>> {
        listOf(
            BilletWebTicketGateway(httpClient = get()),
        )
    }
}

val ticketingModule = module {
    includes(ticketingIntegrationsModule)

    single<TicketRepository> {
        TicketRepositoryExposed(gateways = get())
    }
}
