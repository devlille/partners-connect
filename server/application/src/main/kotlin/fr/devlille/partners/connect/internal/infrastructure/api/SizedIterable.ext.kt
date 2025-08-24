package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.v1.jdbc.SizedIterable

fun <T> SizedIterable<T>.paginated(page: Int, pageSize: Int): SizedIterable<T> {
    if (page < 1) {
        throw BadRequestException("Page number must be greater than 0")
    }
    if (pageSize < 1) {
        throw BadRequestException("Page size must be greater than 0")
    }
    val offset = ((page - 1) * pageSize).toLong()
    return this.limit(count = pageSize).offset(start = offset)
}

fun <T> List<T>.toPaginatedResponse(total: Long, page: Int, pageSize: Int): PaginatedResponse<T> = PaginatedResponse(
    items = this,
    page = page,
    pageSize = pageSize,
    total = total.toLong(),
)
