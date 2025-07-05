package fr.devlille.partners.connect.internal.infrastructure.system

object SystemVarEnv {
    val owner: String = System.getenv("OWNER") ?: ""
    object Exposed {
        val dbUrl: String = System.getenv("EXPOSED_DB_URL") ?: "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1"
        val dbDriver: String = System.getenv("EXPOSED_DB_DRIVER") ?: "org.h2.Driver"
        val dbUser: String = System.getenv("EXPOSED_DB_USER") ?: ""
        val dbPassword: String = System.getenv("EXPOSED_DB_PASSWORD") ?: ""
    }
    object GoogleProvider {
        val clientId: String = System.getenv("GOOGLE_CLIENT_ID")
        val clientSecret: String = System.getenv("GOOGLE_CLIENT_SECRET")
    }
}
