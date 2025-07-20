package fr.devlille.partners.connect.internal.infrastructure.bucket

interface Storage {
    fun download(filename: String): ByteArray?

    fun upload(filename: String, content: ByteArray, mimeType: MimeType): Upload

    fun delete(filename: String)
}

enum class MimeType(val value: String, val extension: String) {
    PNG("image/png", "png"),
    JPG("image/jpeg", "jpg"),
    JPEG("image/jpeg", "jpeg"),
    GIF("image/gif", "gif"),
    SVG("image/svg+xml", "svg"),
    WEBP("image/webp", "webp"),
    JSON("application/json", "json"),
    OCTET_STREAM("application/octet-stream", "bin"),
}

data class Upload(
    val bucketName: String,
    val filename: String,
    val url: String,
)
