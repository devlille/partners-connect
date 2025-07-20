package fr.devlille.partners.connect.internal.infrastructure.bindings

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.StorageOptions
import fr.devlille.partners.connect.internal.infrastructure.bucket.GoogleCloudStorage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import org.koin.dsl.module
import com.google.cloud.storage.Storage as CloudStorage

val storageModule = module {
    single<CloudStorage> {
        StorageOptions.getDefaultInstance().toBuilder().run {
            setProjectId(SystemVarEnv.projectId)
            setCredentials(GoogleCredentials.getApplicationDefault())
            build()
        }.service
    }
    single<Storage> { GoogleCloudStorage(get(), SystemVarEnv.projectId) }
}
