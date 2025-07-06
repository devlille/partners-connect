package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleUserInfo
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json

val mockedAdminUser = GoogleUserInfo(
    sub = "1234567890",
    name = "John Doe",
    givenName = "John",
    familyName = "Doe",
    picture = "https://example.com/john_doe.jpg",
    email = "john.doe@contact.com",
    emailVerified = true,
    hd = "contact.com",
)

val mockEngine = MockEngine { request ->
    if (request.isGoogleProvider()) {
        if (request.headers[HttpHeaders.Authorization]?.contains("invalid") == true) {
            respond(
                content = """{"error":"Unauthorized"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        } else {
            respond(
                content = Json.encodeToString(mockedAdminUser),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    } else {
        TODO("Handle other providers or requests")
    }
}

private fun HttpRequestData.isGoogleProvider(): Boolean = url.host == "openidconnect.googleapis.com"
