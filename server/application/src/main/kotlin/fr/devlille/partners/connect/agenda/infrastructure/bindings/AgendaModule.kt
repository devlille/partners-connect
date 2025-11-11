package fr.devlille.partners.connect.agenda.infrastructure.bindings

import fr.devlille.partners.connect.agenda.application.AgendaRepositoryExposed
import fr.devlille.partners.connect.agenda.domain.AgendaRepository
import fr.devlille.partners.connect.agenda.infrastructure.gateways.OpenPlannerAgendaGateway
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import org.koin.dsl.module

val agendaModule = module {
    includes(networkClientModule)

    single<AgendaRepository> {
        AgendaRepositoryExposed(
            gateways = listOf(
                OpenPlannerAgendaGateway(httpClient = get()),
            ),
        )
    }
}
