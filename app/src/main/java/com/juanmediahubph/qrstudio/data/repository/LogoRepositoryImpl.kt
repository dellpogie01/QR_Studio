package com.juanmediahubph.qrstudio.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.juanmediahubph.qrstudio.di.IoDispatcher
import com.juanmediahubph.qrstudio.domain.repository.LogoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class LogoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LogoRepository {

    private val logoDirectory: File
        get() = File(context.filesDir, "logos")

    override suspend fun importLogo(uri: Uri): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                error("Unsupported image")
            }
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_SOURCE_SIZE ||
                bounds.outHeight / sampleSize > MAX_SOURCE_SIZE
            ) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: error("Could not read image")

            val largest = maxOf(decoded.width, decoded.height)
            val scaled = if (largest > MAX_STORED_SIZE) {
                val scale = MAX_STORED_SIZE.toFloat() / largest
                decoded.scale(
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                ).also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }

            val directory = logoDirectory.apply { if (!exists()) mkdirs() }
            val target = File(directory, "logo_${UUID.randomUUID()}.png")
            FileOutputStream(target).use { output ->
                check(scaled.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode image" }
            }
            scaled.recycle()
            target.absolutePath
        }
    }

    override suspend fun deleteLogo(path: String?): Unit = withContext(ioDispatcher) {
        if (path.isNullOrBlank()) return@withContext
        runCatching {
            val directory = logoDirectory.canonicalFile
            val file = File(path).canonicalFile
            if (file.parentFile == directory && file.isFile) file.delete()
        }
        Unit
    }

    override suspend fun clearAll(): Unit = withContext(ioDispatcher) {
        runCatching { logoDirectory.listFiles()?.forEach { it.delete() } }
        Unit
    }

    private companion object {
        const val MAX_SOURCE_SIZE = 1024
        const val MAX_STORED_SIZE = 512
    }
}
