package fr.devlille.partners.connect.internal.infrastructure.api

/**
 * Examples of how to use the new structured error system in the partners-connect API.
 */

fun basicUsageExample() {
    // Old way - still works for backward compatibility
    throw ForbiddenException("You don't have permission to edit this event")

    // New way - with structured error code and metadata
    throw ForbiddenException(
        code = ErrorCode.NO_EDIT_PERMISSION,
        message = "You don't have permission to edit this event",
        meta = mapOf(
            "resource" to "event",
            "action" to "edit",
            "eventId" to "12345",
            "requiredRole" to "admin",
        ),
    )
}

fun partnershipErrorExamples() {
    // Partnership not found with context
    throw io.ktor.server.plugins.NotFoundException("Partnership not found")

    // Better: Use custom exceptions with structured data
    throw ForbiddenException(
        code = ErrorCode.PARTNERSHIP_ALREADY_EXISTS,
        message = "Partnership already exists for this event and company",
        meta = mapOf(
            "eventId" to "event-123",
            "companyId" to "company-456",
            "existingPartnershipId" to "partnership-789",
        ),
    )
}

fun authenticationErrorExamples() {
    // Token missing
    throw UnauthorizedException(
        code = ErrorCode.TOKEN_MISSING,
        message = "Authentication token is required",
        meta = mapOf(
            "header" to "Authorization",
            "expectedFormat" to "Bearer <token>",
            "endpoint" to "/api/protected-resource",
        ),
    )

    // Invalid permissions
    throw ForbiddenException(
        code = ErrorCode.NO_EDIT_PERMISSION,
        message = "User does not have edit permissions for this organization",
        meta = mapOf(
            "userId" to "user-123",
            "orgSlug" to "devlille",
            "requiredPermission" to "edit",
            "currentPermissions" to "read",
        ),
    )
}
