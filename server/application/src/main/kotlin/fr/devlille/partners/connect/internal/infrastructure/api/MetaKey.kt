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
    
    // Business Entities
    val EVENT = MetaKey("event")
    val COMPANY = MetaKey("company")
    val EXISTING_COMPANY = MetaKey("existing_company")
    val PARTNERSHIP_ID = MetaKey("partnership_id")
    val LOCATION = MetaKey("location")
    
    // System & Resources
    val RESOURCE_PATH = MetaKey("resource_path")
    val FIELD = MetaKey("field")
    val OPERATION = MetaKey("operation")
    val ACCESS_TYPE = MetaKey("access_type")
    val ID = MetaKey("id")
    val INVOICE_STATUS = MetaKey("invoice_status")
    val REQUIRED_STATUS = MetaKey("required_status")
    val RESOURCE = MetaKey("resource")
    val SLUG = MetaKey("slug")
    val SUPPORTED = MetaKey("supported")
    val MISSING_FIELDS = MetaKey("missing_fields")
    val AVAILABLE_TICKETS = MetaKey("available_tickets")
    val REQUESTED_TICKETS = MetaKey("requested_tickets")
    val ACTION = MetaKey("action")
    val CONTENT_TYPE = MetaKey("content_type")
    val VALUE = MetaKey("value")
    val EXPECTED_FORMAT = MetaKey("expected_format")
}
