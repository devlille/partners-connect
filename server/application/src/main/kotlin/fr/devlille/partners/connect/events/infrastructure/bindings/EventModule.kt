package fr.devlille.partners.connect.events.infrastructure.bindings

import fr.devlille.partners.connect.events.application.EventRepositoryDefault
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.koin.dsl.module

val eventModule = module {
    single<EventRepository> {
        EventRepositoryDefault(EventsTable)
    }
}
