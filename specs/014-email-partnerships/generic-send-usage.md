# Generic Send Function Usage Examples

The new generic `send()` functions provide a simpler way to send notifications without wrapping everything in `NotificationVariables`. This is useful for custom notification scenarios that don't fit the template-based pattern.

## Interface Changes

### NotificationGateway Interface

Added new `send()` overload:

```kotlin
suspend fun send(
    integrationId: UUID,
    header: String,
    body: String,
    destinations: List<Destination>,
    metadata: Map<String, Any>? = null,
): Boolean
```

### NotificationRepository Interface

Added new `sendGeneric()` method:

```kotlin
suspend fun sendGeneric(
    eventSlug: String,
    provider: IntegrationProvider,
    header: String,
    body: String,
    destinations: List<Destination>,
    metadata: Map<String, Any>? = null,
)
```

## Usage Examples

### Example 1: Using sendGeneric() from a Route

```kotlin
// In a route handler
post("/orgs/{orgSlug}/events/{eventSlug}/partnerships/email") {
    val eventSlug = call.parameters.eventSlug
    val request = call.receive<SendPartnershipEmailRequest>()
    
    // Prepare recipients
    val destinations = recipients.map { email ->
        Destination(
            identifier = email,
            metadata = mapOf("name" to "") // Optional name
        )
    }
    
    // Prepare metadata with from/cc addresses
    val metadata = mapOf(
        "from" to mapOf(
            "email" to "organizer@example.com",
            "name" to "John Organizer"
        ),
        "cc" to listOf(
            mapOf(
                "email" to "event@example.com",
                "name" to "Event Name"
            )
        )
    )
    
    // Send via generic method
    notificationRepository.sendGeneric(
        eventSlug = eventSlug,
        provider = IntegrationProvider.MAILJET,
        header = "[Event Name] ${request.subject}",
        body = request.htmlBody,
        destinations = destinations,
        metadata = metadata
    )
}
```

### Example 2: Direct Gateway Usage

```kotlin
// If you have the integration ID directly
val destinations = listOf(
    Destination(
        identifier = "user1@example.com",
        metadata = mapOf("name" to "User One")
    ),
    Destination(
        identifier = "user2@example.com",
        metadata = mapOf("name" to "User Two")
    )
)

val metadata = mapOf(
    "from" to mapOf(
        "email" to "sender@example.com",
        "name" to "Sender Name"
    )
)

val success = mailjetGateway.send(
    integrationId = integrationId,
    header = "Important Update",
    body = "<h1>Hello</h1><p>This is the message body.</p>",
    destinations = destinations,
    metadata = metadata
)
```

## Comparison: Old vs New Approach

### Old Approach (Template-based with NotificationVariables.CustomEmail)

```kotlin
val variables = NotificationVariables.CustomEmail(
    language = "en",
    event = eventWithOrg,
    company = Company(...), // Dummy company required
    from = EmailContact("sender@example.com", "Sender"),
    to = listOf(EmailContact("recipient@example.com", "Recipient")),
    cc = listOf(EmailContact("cc@example.com", "CC")),
    subject = "Subject",
    htmlBody = "<p>Body</p>"
)

gateway.send(integrationId, variables)
```

### New Approach (Direct generic send)

```kotlin
val destinations = listOf(
    Destination("recipient@example.com", mapOf("name" to "Recipient"))
)

val metadata = mapOf(
    "from" to mapOf("email" to "sender@example.com", "name" to "Sender"),
    "cc" to listOf(mapOf("email" to "cc@example.com", "name" to "CC"))
)

gateway.send(
    integrationId = integrationId,
    header = "Subject",
    body = "<p>Body</p>",
    destinations = destinations,
    metadata = metadata
)
```

## Benefits

1. **Simpler**: No need to create dummy Company objects or wrap in NotificationVariables
2. **More Direct**: Maps more closely to the underlying provider API
3. **Flexible**: Metadata map can contain any provider-specific data
4. **Composable**: Works well with existing sendMail() for backward compatibility

## Metadata Structure for Mailjet

The Mailjet gateway expects the following metadata structure:

```kotlin
mapOf(
    "from" to mapOf(
        "email" to "sender@example.com",  // Required
        "name" to "Sender Name"            // Optional
    ),
    "cc" to listOf(                        // Optional
        mapOf(
            "email" to "cc@example.com",   // Required
            "name" to "CC Name"             // Optional
        )
    )
)
```

## Migration Path

The existing `sendMail()` method continues to work and uses `NotificationVariables.CustomEmail` internally. You can gradually migrate to the new `sendGeneric()` method for new features or when refactoring existing code.

Both methods are supported and use the same underlying gateway infrastructure.
