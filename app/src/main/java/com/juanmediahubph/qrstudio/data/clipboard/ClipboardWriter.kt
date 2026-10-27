package com.juanmediahubph.qrstudio.data.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.domain.repository.PayloadClipboard
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) : PayloadClipboard {
    override fun copy(text: String, sensitive: Boolean): Boolean {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), text)
        if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        return try {
            manager.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}
