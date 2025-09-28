package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.server.plugins.BadRequestException

class MissingRequestHeaderException(headerName: String) :
    BadRequestException("Request header $headerName is missing")

open class ValidationException(
    parameterName: String,
    message: String,
) : BadRequestException("Request parameter '$parameterName' is invalid: $message")

class RequestBodyValidationException(val errors: List<String>, message: String, cause: Throwable?) :
    BadRequestException(message, cause)

class EmptyStringValidationException(parameterName: String) :
    ValidationException(parameterName, "must not be empty")

class EmptyListValidationException(parameterName: String) :
    ValidationException(parameterName, "must not be an empty list")

class URLValidationException(parameterName: String) :
    ValidationException(parameterName, "must be a valid URL")

class MustBePositiveValidationException(parameterName: String) :
    ValidationException(parameterName, "must be a positive number")
