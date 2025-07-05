package fr.devlille.partners.connect.internal.infrastructure.bindings

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
import org.koin.core.scope.Scope
import org.koin.dsl.module

val Scope.getHttpClientEngine: HttpClientEngine get() = get()

val networkEngineModule = module {
    single<HttpClientEngine> { Java.create() }
}
