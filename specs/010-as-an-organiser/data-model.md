# Phase 1: Data Model Design

## Existing Entities (Leverage)

### Speaker Entity (agenda/infrastructure/db/SpeakerEntity)
```kotlin
// EXISTING - from SpeakersTable
- id: UUID (Primary key)
- externalId: String (OpenPlanner ID, unique index)
- name: String (Speaker name)
- biography: String? (Speaker bio)
- photoUrl: String? (Avatar URL)
- jobTitle: String? (Professional title)
- pronouns: String? (Preferred pronouns)
- eventId: UUID (Foreign key to events)
- companyId: UUID? (Foreign key to companies, nullable)
- createdAt: LocalDateTime (Audit timestamp)
```

### Partnership Entity (partnership/infrastructure/db)
```kotlin
// EXISTING - from partnerships domain
- id: UUID (Primary key)  
- status: PartnershipStatus (APPROVED required for speaker attachment)
- companyId: UUID (Foreign key to companies)
- eventId: UUID (Foreign key to events)
- // ... other partnership fields
```

## New Entity: Speaker-Partnership Association

### SpeakerPartnershipTable (NEW)
```kotlin
object SpeakerPartnershipTable : UUIDTable("speaker_partnerships") {
    val speakerId = reference("speaker_id", SpeakersTable)
    val partnershipId = reference("partnership_id", PartnershipTable)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    
    // Composite unique index: one speaker per partnership
    init {
        uniqueIndex(speakerId, partnershipId)
    }
}
```

### SpeakerPartnershipEntity (NEW)
```kotlin
class SpeakerPartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpeakerPartnershipEntity>(SpeakerPartnershipTable)
    
    var speaker by SpeakerEntity referencedOn SpeakerPartnershipTable.speakerId
    var partnership by PartnershipEntity referencedOn SpeakerPartnershipTable.partnershipId
    var createdAt by SpeakerPartnershipTable.createdAt
}
```

## Domain Model Extensions

### Speaker Domain Entity (NEW - agenda/domain package)
```kotlin
/**
 * Domain representation of a speaker
 * Located in: server/application/src/main/kotlin/fr/devlille/partners/connect/agenda/domain/Speaker.kt
 */
@Serializable
data class Speaker(
    val id: String, // UUID as string for API serialization
    val name: String,
    val biography: String?,
    @SerialName("job_title")
    val jobTitle: String?,
    @SerialName("photo_url") 
    val photoUrl: String?,
    val pronouns: String?
)
```

### SpeakerPartnership Domain Entity (NEW)
```kotlin
/**
 * Domain representation of a speaker-partnership association
 */
@Serializable
data class SpeakerPartnership(
    val id: String, // UUID as string for API serialization
    @SerialName("speaker_id")
    val speakerId: String,
    @SerialName("partnership_id") 
    val partnershipId: String,
    @SerialName("created_at")
    val createdAt: LocalDateTime // Serializable with kotlinx.serialization
)
```

### PartnershipSpeakerRepository Interface (NEW)
```kotlin
interface PartnershipSpeakerRepository {
    /**
     * Attaches a speaker to a partnership if partnership is approved
     * @throws NotFoundException if speaker or partnership not found
     * @throws ConflictException if speaker already attached to partnership  
     * @throws ForbiddenException if partnership not approved
     */
    fun attachSpeaker(partnershipId: UUID, speakerId: UUID): SpeakerPartnership
    
    /**
     * Removes speaker from partnership
     * @throws NotFoundException if association not found
     */
    fun detachSpeaker(partnershipId: UUID, speakerId: UUID)
}
```

### Enhanced PartnershipDetail Integration
```kotlin
// EXISTING PartnershipDetail class will be enhanced to include speakers
// Location: partnership domain package
@Serializable
data class PartnershipDetail(
    // ... existing fields
    val speakers: List<Speaker> = emptyList() // NEW field - clean domain entities
)

// EXISTING repository method to be enhanced
interface PartnershipRepository {
    /**
     * Enhanced to include attached speakers in partnership details
     */
    fun getByIdDetailed(eventSlug: String, partnershipId: UUID): PartnershipDetail
    
    // ... other existing methods
}
```

## Request/Response DTOs

### AttachSpeakerRequest (NEW)
```kotlin
@Serializable
data class AttachSpeakerRequest(
    // No fields needed - speakerId and partnershipId from URL path
)
```

### Attach Speaker Response
```kotlin
// POST /partnerships/{partnershipId}/speakers/{speakerId}
// Returns: SpeakerPartnership domain entity directly (201 Created)
// The SpeakerPartnership class is already @Serializable and handles the response
```

### Detach Speaker Response
```kotlin
// DELETE /partnerships/{partnershipId}/speakers/{speakerId} 
// Returns: Empty response body with HTTP 204 No Content
// No response DTO needed
```

### Enhanced PartnershipDetail Response
```kotlin
// EXISTING PartnershipDetail response will include speakers
// GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}
// The existing getByIdDetailed endpoint automatically returns speakers  
// Speaker entities embedded directly with snake_case serialization via @SerialName
// No additional response DTOs needed - clean domain entities handle serialization
```

### Agenda Retrieval Response (NEW)
```kotlin
/**
 * Response for retrieving imported agenda data
 * GET /orgs/{orgSlug}/events/{eventSlug}/agenda
 */
@Serializable
data class AgendaResponse(
    val sessions: List<Session>,
    val speakers: List<Speaker>
)

/**
 * Enhanced Session domain entity for agenda response
 */
@Serializable
data class Session(
    val id: String,
    val name: String,
    val abstract: String?,
    @SerialName("start_time")
    val startTime: LocalDateTime?,
    @SerialName("end_time")
    val endTime: LocalDateTime?,
    @SerialName("track_name")
    val trackName: String?,
    val language: String?
)
```
```

## Validation Rules

### Business Rules
1. **Partnership Eligibility**: Only APPROVED partnerships can attach speakers
2. **Event Scope**: Speakers can only be attached to partnerships for the same event
3. **Unique Association**: One speaker-partnership pair maximum (enforced by unique index)
4. **Existence Validation**: Both speaker and partnership must exist before association

### Database Constraints  
1. **Foreign Key Cascade**: NO ACTION on delete (preserve audit trail)
2. **Not Null**: All required fields (speakerId, partnershipId, createdAt)
3. **Unique Index**: (speakerId, partnershipId) composite uniqueness
4. **Index Performance**: Indexes on speakerId and partnershipId for query optimization

## Data Flow

### Speaker Attachment Flow
1. Validate partnership exists and status == APPROVED
2. Validate speaker exists and belongs to partnership's event  
3. Check for existing association (409 Conflict if found)
4. Create SpeakerPartnership domain entity with current timestamp
5. Return success response with association details

### Enhanced Partnership Detail Flow
1. Existing `getByIdDetailed(eventSlug, partnershipId)` call
2. Repository implementation fetches partnership data
3. Repository automatically includes attached speakers by querying speaker-partnership associations
4. Return enhanced PartnershipDetail with speakers list populated from SpeakerEntity

### Query Patterns
1. **Enhanced Partnership Details**: Automatic inclusion of speakers in existing partnership detail response
2. **Speaker-Partnership Association**: Simple attach/detach operations for managing relationships
3. **Organizer View**: All speaker information visible through enhanced partnership details endpoint