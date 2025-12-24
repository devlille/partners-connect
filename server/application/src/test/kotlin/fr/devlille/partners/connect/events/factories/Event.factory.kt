package fr.devlille.partners.connect.events.factories

import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import kotlinx.datetime.LocalDateTime

@Suppress("LongParameterList")
fun createEvent(
    name: String = "DevLille 2025",
    startTime: LocalDateTime = LocalDateTime.parse("2025-06-13T18:00:00"),
    endTime: LocalDateTime = LocalDateTime.parse("2025-06-12T09:00:00"),
    submissionStartTime: LocalDateTime = LocalDateTime.parse("2025-01-01T00:00:00"),
    submissionEndTime: LocalDateTime = LocalDateTime.parse("2025-03-01T23:59:59"),
    address: String = "Lille Grand Palais, Lille, France",
    phone: String = "+33 6 12 34 56 78",
    email: String = "contact@mail.com",
): Event = Event(
    name = name,
    startTime = startTime,
    endTime = endTime,
    submissionStartTime = submissionStartTime,
    submissionEndTime = submissionEndTime,
    address = address,
    contact = Contact(phone = phone, email = email),
)
