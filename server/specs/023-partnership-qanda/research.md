# Research: Partnership Q&A Game

**Feature**: 023-partnership-qanda  
**Date**: 2026-04-02

## Research Tasks

### 1. Q&A Configuration Storage Strategy

**Question**: Should Q&A config be stored as new columns on `EventsTable` or in a separate table?

**Decision**: Add columns directly to `EventsTable`.

**Rationale**: The Q&A config consists of only 3 fields (`qanda_enabled`, `qanda_max_questions`, `qanda_max_answers`). This is a small, fixed set of scalar values tightly coupled to the event. Adding a separate table would introduce unnecessary join complexity for a 1:1 relationship.

**Alternatives considered**:
- Separate `EventQandaConfigTable` — rejected because it adds join overhead for a 1:1 relationship with only 3 columns. The booth plan image URL is stored directly on EventsTable following the same rationale.

### 2. Q&A Domain Placement

**Question**: Should Q&A be a new top-level domain module or part of the `partnership` module?

**Decision**: Part of the `partnership` domain module.

**Rationale**: Questions belong to partnerships (FK to `PartnershipsTable`). The CRUD pattern (list, create, update, delete) is identical to booth activities which already live in the `partnership` module. Placing Q&A in the same module follows the established pattern and avoids unnecessary module proliferation.

**Alternatives considered**:
- New `qanda/` top-level module — rejected because it would create a module that depends on partnership data, violating the "modules MUST remain decoupled" principle. Q&A questions are a partnership sub-resource.

### 3. Public Endpoint Pattern

**Question**: How should the public event questions endpoint be structured?

**Decision**: Use `/events/{eventSlug}/qanda/questions` as a public route (no `AuthorizedOrganisationPlugin`).

**Rationale**: The spec clarifies this is a public unauthenticated endpoint. It follows the existing pattern of `/events/{eventSlug}/...` for public event data (e.g., `/events/{eventSlug}/sponsoring/packs`).

**Alternatives considered**:
- Webhook-only delivery — rejected because the spec explicitly requires a fetch endpoint for external game services.
- Authenticated endpoint — rejected per clarification session (Q&A data is non-sensitive company trivia).

### 4. Partner CRUD Pattern

**Question**: Which existing pattern should Q&A CRUD follow?

**Decision**: Follow the booth activities pattern exactly.

**Rationale**: Booth activities have the same shape: partnership-scoped, public CRUD endpoints under `/events/{eventSlug}/partnerships/{partnershipId}/...`, no org-level auth needed (partners manage their own resources). The route structure, repository interface, entity design, and test patterns can be replicated directly.

**Alternatives considered**:
- Org-scoped routes only — rejected because partners need self-service access from their partnership space without organiser auth.

### 5. Webhook Payload Extension

**Question**: How should Q&A data be added to the webhook payload?

**Decision**: Add a `questions` field to `WebhookPayload` following the same pattern as `activities` (booth activities).

**Rationale**: The current payload already fetches booth activities by partnership ID and includes them. Q&A questions follow the identical pattern: fetch by partnership ID, map to domain, include in payload. The field defaults to an empty list when no questions exist.

**Alternatives considered**:
- Separate webhook event type for Q&A — rejected because the spec says Q&A data should be included in existing partnership webhook triggers, not as a separate event.

### 6. Event Update DTO Extension

**Question**: How should Q&A config be added to the event create/update flow?

**Decision**: Add optional Q&A fields to the `Event` data class with nullable defaults. Update the `create_event.schema.json` schema to allow the new fields.

**Rationale**: The `Event` DTO is used for both create and update via `call.receive<Event>(schema = "create_event.schema.json")`. Making the Q&A fields nullable with defaults ensures backward compatibility — existing event creation/update calls without Q&A fields continue to work.

**Alternatives considered**:
- Separate update endpoint — rejected per clarification session (Q&A config is part of the event update payload).
