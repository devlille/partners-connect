package fr.devlille.partners.connect.internal.infrastructure.api

/**
 * Object class that defines all available meta keys for structured error responses.
 * Uses snake_case naming convention for consistency with JSON APIs.
 */
object MetaKey {
    const val EMAIL = "email"
    const val ORGANISATION = "organisation"
    const val REQUIRED_ROLE = "required_role"
    const val HEADER = "header"
    const val SESSION_AVAILABLE = "session_available"
    const val MEDIA_TYPE = "media_type"
    const val SUPPORTED_TYPES = "supported_types"
    const val EVENT = "event"
    const val COMPANY = "company"
    const val HTTP_STATUS = "http_status"
    const val URL = "url"
    const val RESOURCE_PATH = "resource_path"
    const val LOCATION = "location"
    const val EXISTING_COMPANY = "existing_company"
    const val PARTNERSHIP_ID = "partnership_id"
}
