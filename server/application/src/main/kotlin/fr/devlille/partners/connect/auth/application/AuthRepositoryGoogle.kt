package fr.devlille.partners.connect.auth.application

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleProvider

class AuthRepositoryGoogle(
    private val googleProvider: GoogleProvider,
) : AuthRepository {
    override suspend fun getUserInfo(token: String): UserInfo {
        val googleUserInfo = googleProvider.getGoogleUserInfo(token)
        return UserInfo(
            givenName = googleUserInfo.givenName ?: "Unknown",
            firstName = googleUserInfo.familyName ?: "Unknown",
            pictureUrl = googleUserInfo.picture,
            email = googleUserInfo.email,
        )
    }
}
