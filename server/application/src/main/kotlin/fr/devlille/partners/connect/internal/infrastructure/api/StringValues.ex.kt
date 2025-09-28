package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.util.StringValues

/**
 * Extension function to get a parameter value from StringValues or throw BadRequestException2 if not found.
 * @param name The name of the parameter to retrieve.
 * @return The value of the parameter.
 * @throws MissingRequestParameterException if the parameter is missing.
 */
fun StringValues.getValue(name: String): String = this[name]
    ?: throw MissingRequestParameterException(parameterName = name)
