package fr.devlille.partners.connect.events.infrastructure.bindings

import fr.devlille.partners.connect.events.application.EventRepositoryExposed
import fr.devlille.partners.connect.events.application.EventStorageRepositoryGoogleStorage
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventStorageRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.koin.dsl.module

val eventModule = module {
    single<EventRepository> {
        EventRepositoryExposed(EventEntity, get())
    }
    single<EventStorageRepository> {
        EventStorageRepositoryGoogleStorage(get())
    }
}
