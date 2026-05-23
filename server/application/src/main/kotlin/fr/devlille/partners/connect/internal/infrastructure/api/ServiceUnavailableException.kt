package fr.devlille.partners.connect.internal.infrastructure.api

class ServiceUnavailableException(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)
