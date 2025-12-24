package fr.devlille.partners.connect.auth.infrastructure.providers

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

val mockedAdminUser = GoogleUserInfo(
    givenName = "John",
    familyName = "Doe",
    picture = "https://example.com/john_doe.jpg",
    email = "john.doe@contact.com",
)

fun HttpRequestData.isGoogleProvider(): Boolean = url.host == "openidconnect.googleapis.com"

val MockRequestHandleScope.unAuthorizedGoogleProviderResponse: HttpResponseData
    get() = respond(
        content = """{"error":"Unauthorized"}""",
        status = HttpStatusCode.Unauthorized,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

fun MockRequestHandleScope.mockedGoogleProviderResponse(
    userInfo: GoogleUserInfo = mockedAdminUser,
): HttpResponseData = respond(
    content = Json.encodeToString(value = userInfo),
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType, "application/json"),
)

fun MockRequestHandleScope.mockedGoogleProviderResponse(
    userId: UUID,
): HttpResponseData {
    val user = transaction { UserEntity[userId] }
    val userInfo = GoogleUserInfo(
        givenName = user.name?.split(" ")?.firstOrNull() ?: "",
        familyName = user.name?.split(" ")?.lastOrNull() ?: "",
        picture = user.pictureUrl ?: "",
        email = user.email,
    )
    return respond(
        content = Json.encodeToString(value = userInfo),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}
