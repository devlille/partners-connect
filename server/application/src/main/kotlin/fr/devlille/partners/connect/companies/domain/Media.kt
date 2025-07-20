package fr.devlille.partners.connect.companies.domain

import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Media(
    val original: String,
    @SerialName("png_1000")
    val png1000: String,
    @SerialName("png_500")
    val png500: String,
    @SerialName("png_250")
    val png250: String,
)

class MediaBinary(
    val mimeType: MimeType,
    val original: ByteArray,
    val png1000: ByteArray,
    val png250: ByteArray,
    val png500: ByteArray,
)
