# Phase 0: Research & Technical Decisions

## OpenPlanner Integration Analysis

**Decision**: Leverage existing OpenPlanner gateway implementation in agenda package  
**Rationale**: Complete OpenPlanner integration already exists with error handling, authentication, and data mapping. Avoids duplication and maintains consistency.  
**Implementation**: Use existing `AgendaRepository.fetchAndStore()` without modification, enhance error handling to prevent database impact on OpenPlanner failures.

## Speaker-Partnership Data Model

**Decision**: Create minimal speaker-partnership association with clean domain entities  
**Rationale**: SpeakersTable and PartnershipTable already exist. New association table with Speaker domain entity (no internal fields exposed) enables clean API responses.  
**Implementation**: SpeakerPartnershipTable with composite unique index, Speaker domain entity in agenda/domain package without externalId/eventId/companyId (context provided by containing objects).

## API Endpoint Architecture

**Decision**: Create minimal public endpoints (POST attach, DELETE detach) with speakers integrated into existing partnership details  
**Rationale**: Speaker attachment is partnership functionality. Speakers are best viewed as part of partnership context rather than separate resources.  
**Implementation**: New `PartnershipSpeakerRoutes.kt` with two endpoints: POST returns SpeakerPartnership domain entity (201), DELETE returns empty response (204). Speaker lists available through enhanced PartnershipDetail.speakers field.

## Partnership Validation Strategy

**Decision**: Use existing partnership domain entities with approval status validation  
**Rationale**: Clarification confirmed "organizer-approved partnerships" as eligibility criteria. Existing partnership entities already track approval status.  
**Implementation**: Repository validates partnership.status == APPROVED before allowing speaker attachment.

## Error Handling for OpenPlanner Failures

**Decision**: Implement transaction rollback pattern for OpenPlanner API failures  
**Rationale**: User requirement specifies "simply stop workflow without database impact" on OpenPlanner errors.  
**Implementation**: Wrap agenda import in database transaction, catch OpenPlanner exceptions, rollback transaction, return HTTP 503 Service Unavailable.

## JSON Schema Validation

**Decision**: Implement minimal schema validation for AttachSpeakerRequest (empty object) using call.receive<T>(schema) pattern  
**Rationale**: Constitutional requirement for all API endpoints. AttachSpeakerRequest needs no fields (IDs from URL path), DELETE needs no request body.  
**Implementation**: Create AttachSpeakerRequest.json schema (empty object), use in POST route handler. DELETE endpoint uses no request validation. Both endpoints return domain entities directly (SpeakerPartnership for POST, empty for DELETE).

## Testing Strategy

**Decision**: Contract tests for API schemas + Integration tests for business workflows  
**Rationale**: Constitutional requirement separates contract validation from business logic testing. Enables TDD with failing tests first.  
**Implementation**: Mock factories for speakers/partnerships, separate test files for contract vs integration scenarios.

## Authorization Pattern

**Decision**: Use existing AuthorizedOrganisationPlugin for agenda routes, no auth for public partnership routes  
**Rationale**: Agenda import requires organization permissions, speaker attachment is partnership-scoped (authenticated via partnership ID).  
**Implementation**: Keep agenda routes protected, make partnership speaker routes public with partnership validation.

## Database Performance

**Decision**: Leverage existing indexes on speakers.event_id and foreign key constraints  
**Rationale**: Current schema already optimized for event-based speaker queries. No additional indexing required.  
**Implementation**: Use existing Exposed entity relationships for efficient queries.

## API Documentation

**Decision**: Extend existing OpenAPI specification with new partnership endpoints  
**Rationale**: Constitutional requirement for comprehensive API documentation with external schema references.  
**Implementation**: Add new operations to openapi.yaml referencing external JSON schemas for request/response validation.