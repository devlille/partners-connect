package fr.devlille.partners.connect.auth.domain

interface AuthRepository {
    suspend fun getUserInfo(token: String): UserInfo
}
