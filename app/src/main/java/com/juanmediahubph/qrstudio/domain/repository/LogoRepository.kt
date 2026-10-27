package com.juanmediahubph.qrstudio.domain.repository

import android.net.Uri

interface LogoRepository {
    /** Copies a picked image into app-private storage and returns its absolute path. */
    suspend fun importLogo(uri: Uri): Result<String>

    suspend fun deleteLogo(path: String?)

    suspend fun clearAll()
}
