package fr.devlille.partners.connect.tickets.infrastructure.bindings

import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.tickets.application.TicketRepositoryExposed
import fr.devlille.partners.connect.tickets.domain.TicketRepository
import fr.devlille.partners.connect.tickets.infrastructure.gateways.BilletWebTicketGateway
import org.koin.dsl.module

val ticketingModule = module {
    includes(networkClientModule)

    single<TicketRepository> {
        TicketRepositoryExposed(
            gateways = listOf(
                BilletWebTicketGateway(httpClient = get()),
            ),
        )
    }
}
