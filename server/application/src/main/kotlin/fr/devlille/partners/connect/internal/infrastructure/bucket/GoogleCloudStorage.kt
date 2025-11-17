package fr.devlille.partners.connect.internal.infrastructure.bucket

import com.google.cloud.storage.Acl
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.Storage as CloudStorage

internal class GoogleCloudStorage(
    private val storage: CloudStorage,
    private val bucketName: String,
) : Storage {
    override fun download(filename: String): ByteArray? = try {
        storage.readAllBytes(BlobId.of(bucketName, filename))
    } catch (_: StorageException) {
        null
    }

    override fun upload(filename: String, content: ByteArray, mimeType: MimeType): Upload {
        val blobId = BlobId.of(bucketName, filename)
        val blobInfo = BlobInfo
            .newBuilder(blobId)
            .setAcl(arrayListOf(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
            .setCacheControl("public")
            .setContentType(mimeType.value)
            .build()
        storage.create(blobInfo, content)
        return Upload(
            bucketName = bucketName,
            filename = filename,
            url = "https://storage.googleapis.com/$bucketName/$filename",
        )
    }

    override fun delete(filename: String) {
        storage.delete(bucketName, filename)
    }
}
