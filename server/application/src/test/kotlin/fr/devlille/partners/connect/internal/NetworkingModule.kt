package fr.devlille.partners.connect.internal

import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module

val mockNetworkingEngineModule = module {
    single<HttpClientEngine> { mockEngine }
}
