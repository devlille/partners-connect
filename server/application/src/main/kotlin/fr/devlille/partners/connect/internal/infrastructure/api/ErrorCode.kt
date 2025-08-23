package fr.devlille.partners.connect.internal.infrastructure.api

/**
 * Enumeration of structured error codes for the API.
 * Each code represents a specific error scenario that can occur in the system.
 * API consumers can use these codes for internationalization and specific error handling.
 */
enum class ErrorCode {
    // General validation errors
    BAD_REQUEST,
    INVALID_UUID_FORMAT,
    MISSING_REQUIRED_PARAMETER,
    UNSUPPORTED_MEDIA_TYPE,

    // Authentication and authorization errors
    UNAUTHORIZED,
    TOKEN_MISSING,
    FORBIDDEN,
    NO_EDIT_PERMISSION,

    // Entity not found errors
    NOT_FOUND,
    USER_NOT_FOUND,
    ORGANISATION_NOT_FOUND,
    EVENT_NOT_FOUND,
    COMPANY_NOT_FOUND,
    PARTNERSHIP_NOT_FOUND,
    SPONSORING_PACK_NOT_FOUND,
    SPONSORING_OPTION_NOT_FOUND,
    INTEGRATION_NOT_FOUND,
    PROVIDER_NOT_FOUND,

    // Business logic errors
    PARTNERSHIP_ALREADY_EXISTS,
    INVALID_PARTNERSHIP_STATUS,
    BILLING_PROCESSING_ERROR,
    NOTIFICATION_DELIVERY_ERROR,
    TICKET_GENERATION_ERROR,

    // File and storage errors
    FILE_UPLOAD_ERROR,
    FILE_NOT_FOUND,
    STORAGE_ERROR,

    // Integration errors
    EXTERNAL_SERVICE_ERROR,
    INTEGRATION_NOT_CONFIGURED,

    // System errors
    INTERNAL_SERVER_ERROR,
    DATABASE_ERROR,
    MIGRATION_ERROR,
}
