package fr.devlille.partners.connect.internal.infrastructure.api

/**
 * Data class representing a meta key with its string key.
 * Provides type-safe access to error metadata keys.
 */
data class MetaKey(val key: String)

/**
 * Object class that defines all available meta keys for structured error responses.
 * Uses snake_case naming convention for consistency with JSON APIs.
 * Keys are organized by logical groups.
 */
object MetaKeys {
    // Authentication & Authorization
    val EMAIL = MetaKey("email")
    val ORGANISATION = MetaKey("organisation")
    val REQUIRED_ROLE = MetaKey("required_role")
    
    // HTTP & Media Related
    val HEADER = MetaKey("header")
    val MEDIA_TYPE = MetaKey("media_type")
    val SUPPORTED_TYPES = MetaKey("supported_types")
    val HTTP_STATUS = MetaKey("http_status")
    val URL = MetaKey("url")
    val CONTENT_TYPE = MetaKey("content_type")
    
    // Business Entities
    val EVENT = MetaKey("event")
    val EVENT_ID = MetaKey("event_id")
    val COMPANY = MetaKey("company")
    val PARTNERSHIP_ID = MetaKey("partnership_id")
    val LOCATION = MetaKey("location")
    val PROVIDER = MetaKey("provider")
    
    // System & Resources
    val RESOURCE_PATH = MetaKey("resource_path")
    val FIELD = MetaKey("field")
    val OPERATION = MetaKey("operation")
    val ID = MetaKey("id")
    val INVOICE_STATUS = MetaKey("invoice_status")
    val REQUIRED_STATUS = MetaKey("required_status")
    val RESOURCE = MetaKey("resource")
    val SUPPORTED = MetaKey("supported")
    val MISSING_FIELDS = MetaKey("missing_fields")
    val AVAILABLE_TICKETS = MetaKey("available_tickets")
    val REQUESTED_TICKETS = MetaKey("requested_tickets")
    val EXPECTED_FORMAT = MetaKey("expected_format")
}
