package fr.devlille.partners.connect.agenda.factories

import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedSpeaker(
    id: UUID = UUID.randomUUID(),
    externalId: String = "speaker-${id.toString().take(8)}",
    name: String = id.toString(),
    biography: String? = "Test speaker biography",
    photoUrl: String? = "https://example.com/photo.jpg",
    jobTitle: String? = "Developer",
    pronouns: String? = "they/them",
    eventId: UUID = UUID.randomUUID(),
    companyId: UUID? = null,
): SpeakerEntity = SpeakerEntity.new(id) {
    this.externalId = externalId
    this.name = name
    this.biography = biography
    this.photoUrl = photoUrl
    this.jobTitle = jobTitle
    this.pronouns = pronouns
    this.event = EventEntity[eventId]
    this.company = companyId?.let { CompanyEntity[it] }
}
