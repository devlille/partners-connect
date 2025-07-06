package fr.devlille.partners.connect.auth.infrastructure.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class GoogleProvider(private val httpClient: HttpClient) {
    suspend fun getGoogleUserInfo(token: String): GoogleUserInfo = httpClient
        .get("https://openidconnect.googleapis.com/v1/userinfo") {
            headers[HttpHeaders.Authorization] = token
            headers[HttpHeaders.Accept] = "application/json"
        }.body()
}

@Suppress("LongParameterList")
@Serializable
class GoogleUserInfo(
    val sub: String,
    val name: String,
    @SerialName("given_name")
    val givenName: String,
    @SerialName("family_name")
    val familyName: String,
    val picture: String,
    val email: String,
    @SerialName("email_verified")
    val emailVerified: Boolean,
    val hd: String,
)
