package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID

fun insertMockedPackOptions(
    packId: UUID = UUID.randomUUID(),
    optionId: UUID = UUID.randomUUID(),
    required: Boolean = true,
) =
    PackOptionsTable.insert {
        it[this.pack] = packId
        it[this.option] = optionId
        it[this.required] = required
    }
