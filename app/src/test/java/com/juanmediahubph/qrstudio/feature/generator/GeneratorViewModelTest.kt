package com.juanmediahubph.qrstudio.feature.generator

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.juanmediahubph.qrstudio.core.util.MainDispatcherRule
import com.juanmediahubph.qrstudio.domain.export.QrExporter
import com.juanmediahubph.qrstudio.domain.model.HistoryFilter
import com.juanmediahubph.qrstudio.domain.model.PdfLayout
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import com.juanmediahubph.qrstudio.domain.model.SavedQr
import com.juanmediahubph.qrstudio.domain.payload.FormError
import com.juanmediahubph.qrstudio.domain.payload.VCardPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.UrlPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.WifiPayloadBuilder
import com.juanmediahubph.qrstudio.domain.qr.EncodedQr
import com.juanmediahubph.qrstudio.domain.repository.LogoRepository
import com.juanmediahubph.qrstudio.domain.repository.PayloadClipboard
import com.juanmediahubph.qrstudio.domain.repository.QrRepository
import com.juanmediahubph.qrstudio.domain.repository.SettingsRepository
import com.juanmediahubph.qrstudio.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneratorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clipboard = RecordingClipboard()
    private val created = mutableListOf<GeneratorViewModel>()

    @After
    fun cancelViewModels() {
        created.forEach { it.viewModelScope.cancel() }
        created.clear()
    }

    private fun viewModel(type: String): GeneratorViewModel = GeneratorViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(Destinations.TYPE_ARG to type, Destinations.ID_ARG to 0L),
        ),
        qrRepository = FakeQrRepository(),
        settingsRepository = FakeSettingsRepository(),
        logoRepository = FakeLogoRepository(),
        exporter = FakeExporter(),
        clipboard = clipboard,
    ).also(created::add)

    @Test
    fun invalidUrl_exposesErrorOnlyAfterTheFieldIsTouched() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel("URL")

        assertEquals(FormError.REQUIRED, viewModel.state.value.errors[UrlPayloadBuilder.URL])
        assertNull(viewModel.state.value.errorFor(UrlPayloadBuilder.URL))

        viewModel.onFieldChange(UrlPayloadBuilder.URL, "not a url")

        assertEquals(FormError.INVALID_URL, viewModel.state.value.errorFor(UrlPayloadBuilder.URL))
    }

    @Test
    fun vCardWithoutNameOrPhone_reportsTheNameError() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel("VCARD")

        viewModel.onFieldChange(VCardPayloadBuilder.NAME, "")

        assertEquals(
            FormError.NAME_OR_PHONE_REQUIRED,
            viewModel.state.value.errorFor(VCardPayloadBuilder.NAME),
        )
    }

    @Test
    fun wifiCopy_marksTheClipboardContentSensitive() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel("WIFI")
        viewModel.onFieldChange(WifiPayloadBuilder.SSID, "Home;Network")
        viewModel.onFieldChange(WifiPayloadBuilder.PASSWORD, "secret")

        viewModel.copyPayload()

        assertEquals("WIFI:T:WPA;S:Home\\;Network;P:secret;H:false;;", clipboard.lastText)
        assertTrue(clipboard.lastSensitive)
    }

    @Test
    fun copyWithoutRequiredFields_callsAttentionToTheForm() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel("URL")

        viewModel.copyPayload()

        assertNull(clipboard.lastText)
        assertTrue(viewModel.state.value.touched.contains(UrlPayloadBuilder.URL))
    }

    @Test
    fun largeLogoScale_isClampedByTheStyleSanitizer() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel("TEXT")

        viewModel.onStyleChange { it.copy(logoScale = 0.9f, quietZone = 50) }

        assertEquals(QrStyle.MAX_LOGO_SCALE, viewModel.state.value.style.logoScale)
        assertEquals(QrStyle.MAX_QUIET_ZONE, viewModel.state.value.style.quietZone)
    }

    private class RecordingClipboard : PayloadClipboard {
        var lastText: String? = null
        var lastSensitive: Boolean = false

        override fun copy(text: String, sensitive: Boolean): Boolean {
            lastText = text
            lastSensitive = sensitive
            return true
        }
    }

    private class FakeQrRepository : QrRepository {
        private val flow = MutableStateFlow<List<SavedQr>>(emptyList())

        override fun observeHistory(query: String, filter: HistoryFilter): Flow<List<SavedQr>> = flow

        override suspend fun getById(id: Long): SavedQr? = null

        override suspend fun save(qr: SavedQr): Long = qr.id

        override suspend fun restore(qr: SavedQr) = Unit

        override suspend fun delete(id: Long) = Unit

        override suspend fun deleteMany(ids: Collection<Long>) = Unit

        override suspend fun setFavorite(id: Long, favorite: Boolean) = Unit

        override suspend fun clearAll() = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow(com.juanmediahubph.qrstudio.domain.model.AppSettings())

        override val settings: Flow<com.juanmediahubph.qrstudio.domain.model.AppSettings> = state

        override suspend fun setTheme(theme: com.juanmediahubph.qrstudio.domain.model.ThemeMode) = Unit

        override suspend fun setDefaultEcLevel(level: com.juanmediahubph.qrstudio.domain.model.EcLevel) = Unit

        override suspend fun setDefaultForeground(argb: Long) = Unit

        override suspend fun setDefaultBackground(argb: Long) = Unit

        override suspend fun setDefaultSize(size: com.juanmediahubph.qrstudio.domain.model.QrSize) = Unit

        override suspend fun setSaveHistory(enabled: Boolean) = Unit
    }

    private class FakeLogoRepository : LogoRepository {
        override suspend fun importLogo(uri: Uri): Result<String> =
            Result.failure(IllegalStateException("Not available in tests"))

        override suspend fun deleteLogo(path: String?) = Unit

        override suspend fun clearAll() = Unit
    }

    private class FakeExporter : QrExporter {
        override suspend fun savePng(
            encoded: EncodedQr,
            style: QrStyle,
            title: String,
        ): Result<Uri> = Result.failure(IllegalStateException("Not available in tests"))

        override suspend fun prepareShareImage(
            encoded: EncodedQr,
            style: QrStyle,
            title: String,
        ): Result<Uri> = Result.failure(IllegalStateException("Not available in tests"))

        override suspend fun savePdf(
            encoded: EncodedQr,
            style: QrStyle,
            title: String,
            type: com.juanmediahubph.qrstudio.domain.model.QrType,
            layout: PdfLayout,
        ): Result<Uri> = Result.failure(IllegalStateException("Not available in tests"))
    }
}
