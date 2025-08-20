package fr.devlille.partners.connect.internal.infrastructure.system

import org.jetbrains.exposed.v1.crypt.Algorithms

object SystemVarEnv {
    val projectId: String = System.getenv("PROJECT_ID") ?: "partners-connect"
    val owner: String = System.getenv("OWNER") ?: ""
    val frontendBaseUrl: String = System.getenv("FRONTEND_BASE_URL") ?: "http://localhost:80"

    object Exposed {
        val dbUrl: String = System.getenv("EXPOSED_DB_URL") ?: "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1"
        val dbDriver: String = System.getenv("EXPOSED_DB_DRIVER") ?: "org.h2.Driver"
        val dbUser: String = System.getenv("EXPOSED_DB_USER") ?: ""
        val dbPassword: String = System.getenv("EXPOSED_DB_PASSWORD") ?: ""
    }

    object Crypto {
        val key: String = System.getenv("CRYPTO_KEY") ?: "passwd"
        val salt: String = System.getenv("CRYPTO_SALT") ?: "4420d1918d"
        val algorithm = Algorithms.AES_256_PBE_GCM(password = key, salt = salt)
    }

    object GoogleProvider {
        val clientId: String = System.getenv("GOOGLE_CLIENT_ID")
        val clientSecret: String = System.getenv("GOOGLE_CLIENT_SECRET")
    }

    object QontoProvider {
        val baseUrl: String = System.getenv("QONTO_BASE_URL") ?: "https://thirdparty.qonto.com"
    }
}
