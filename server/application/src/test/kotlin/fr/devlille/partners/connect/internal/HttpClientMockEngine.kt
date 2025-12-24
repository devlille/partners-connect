package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.auth.infrastructure.providers.isGoogleProvider
import fr.devlille.partners.connect.auth.infrastructure.providers.mockedGoogleProviderResponse
import fr.devlille.partners.connect.auth.infrastructure.providers.unAuthorizedGoogleProviderResponse
import fr.devlille.partners.connect.tickets.infrastructure.providers.isBilletWebProvider
import fr.devlille.partners.connect.tickets.infrastructure.providers.mockedBilletWebProviderResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import java.util.UUID

val mockEngine = MockEngine { request ->
    if (request.isGoogleProvider()) {
        if (request.headers[HttpHeaders.Authorization]?.contains("invalid") == true) {
            unAuthorizedGoogleProviderResponse
        } else {
            mockedGoogleProviderResponse()
        }
    } else {
        TODO("Handle other providers or requests")
    }
}

fun mockEngine(
    userId: UUID,
    nbProductsForTickets: Int = 0,
): MockEngine = MockEngine { request ->
    if (request.isGoogleProvider()) {
        if (request.headers[HttpHeaders.Authorization]?.contains("invalid") == true) {
            unAuthorizedGoogleProviderResponse
        } else {
            mockedGoogleProviderResponse(userId)
        }
    } else if (request.isBilletWebProvider()) {
        mockedBilletWebProviderResponse(nbProducts = nbProductsForTickets)
    } else {
        TODO("Handle other providers or requests")
    }
}
