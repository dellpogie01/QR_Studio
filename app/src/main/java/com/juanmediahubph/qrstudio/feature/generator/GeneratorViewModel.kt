@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.juanmediahubph.qrstudio.feature.generator

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.core.qr.QrPainter
import com.juanmediahubph.qrstudio.data.AppJson
import com.juanmediahubph.qrstudio.domain.export.QrExporter
import com.juanmediahubph.qrstudio.domain.model.PdfLayout
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.model.SavedQr
import com.juanmediahubph.qrstudio.domain.payload.BuiltPayload
import com.juanmediahubph.qrstudio.domain.payload.FormError
import com.juanmediahubph.qrstudio.domain.payload.PayloadBuilderRegistry
import com.juanmediahubph.qrstudio.domain.qr.EncodeResult
import com.juanmediahubph.qrstudio.domain.qr.EncodedQr
import com.juanmediahubph.qrstudio.domain.qr.QrEncoder
import com.juanmediahubph.qrstudio.domain.repository.LogoRepository
import com.juanmediahubph.qrstudio.domain.repository.PayloadClipboard
import com.juanmediahubph.qrstudio.domain.repository.QrRepository
import com.juanmediahubph.qrstudio.domain.repository.SettingsRepository
import com.juanmediahubph.qrstudio.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

enum class GeneratorAction {
    SAVE,
    SHARE,
    PDF,
    FAVORITE,
}

data class GeneratorUiState(
    val type: QrType,
    val fields: Map<String, String>,
    val touched: Set<String> = emptySet(),
    val errors: Map<String, FormError> = emptyMap(),
    val style: QrStyle = QrStyle(),
    val payload: String? = null,
    val builtTitle: String = "",
    val encoded: EncodedQr? = null,
    val preview: Bitmap? = null,
    val isRendering: Boolean = false,
    val tooLong: Boolean = false,
    val encodeFailed: Boolean = false,
    val savedId: Long = 0L,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val working: Set<GeneratorAction> = emptySet(),
    val logoBusy: Boolean = false,
    val draftLoaded: Boolean = false,
)

sealed interface GeneratorEvent {
    data class Snackbar(@param:StringRes val messageRes: Int) : GeneratorEvent

    data class Share(val uri: Uri) : GeneratorEvent
}

private data class GeneratorInput(
    val type: QrType,
    val fields: Map<String, String>,
    val style: QrStyle,
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val qrRepository: QrRepository,
    private val settingsRepository: SettingsRepository,
    private val logoRepository: LogoRepository,
    private val exporter: QrExporter,
    private val clipboard: PayloadClipboard,
) : ViewModel() {

    private val encoder = QrEncoder()
    private val fieldsSerializer = MapSerializer(String.serializer(), String.serializer())
    private val saveMutex = Mutex()
    private val eventsChannel = Channel<GeneratorEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val initialType: QrType =
        QrType.fromNameOrText(savedStateHandle.get<String>(Destinations.TYPE_ARG))
    private val initialId: Long = savedStateHandle.get<Long>(Destinations.ID_ARG) ?: 0L
    private val initialDraft: Pair<Map<String, String>, QrStyle>? = readDraft()
    private var styleTouched = false

    private val _state = MutableStateFlow(
        GeneratorUiState(
            type = initialType,
            fields = initialDraft?.first
                ?: PayloadBuilderRegistry.get(initialType).defaultFields(),
            errors = PayloadBuilderRegistry.get(initialType).validate(
                initialDraft?.first ?: PayloadBuilderRegistry.get(initialType).defaultFields(),
            ),
            style = initialDraft?.second ?: QrStyle(),
            savedId = initialId,
            draftLoaded = initialId == 0L,
        ),
    )
    val state: StateFlow<GeneratorUiState> = _state.asStateFlow()

    private val input = MutableStateFlow(
        GeneratorInput(
            type = _state.value.type,
            fields = _state.value.fields,
            style = _state.value.style,
        ),
    )

    init {
        if (initialId == 0L && initialDraft == null) {
            viewModelScope.launch {
                val defaults = settingsRepository.settings.first().toStyle()
                if (!styleTouched) {
                    applyStyle(defaults)
                }
            }
        }
        if (initialId > 0L) {
            viewModelScope.launch {
                loadSaved(initialId)
            }
        }
        viewModelScope.launch {
            input.debounce(RENDER_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { current -> encodeAndRender(current) }
        }
        viewModelScope.launch {
            input.debounce(AUTOSAVE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { current -> autosave(current) }
        }
    }

    fun onFieldChange(key: String, value: String) {
        val current = _state.value
        val fields = current.fields + (key to value)
        updateForm(fields = fields, style = current.style)
        _state.update { it.copy(touched = it.touched + key) }
    }

    fun onStyleChange(transform: (QrStyle) -> QrStyle) {
        styleTouched = true
        applyStyle(transform(_state.value.style).sanitized())
    }

    fun importLogo(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(logoBusy = true) }
            logoRepository.importLogo(uri)
                .onSuccess { path ->
                    applyStyle(_state.value.style.copy(logoPath = path))
                }
                .onFailure {
                    eventsChannel.send(GeneratorEvent.Snackbar(R.string.snack_logo_failed))
                }
            _state.update { it.copy(logoBusy = false) }
        }
    }

    fun removeLogo() {
        if (_state.value.style.logoPath != null) {
            applyStyle(_state.value.style.copy(logoPath = null))
        }
    }

    fun toggleFavorite() {
        if (GeneratorAction.FAVORITE in _state.value.working) return
        val built = requireBuilt() ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = it.working + GeneratorAction.FAVORITE) }
            try {
                val current = _state.value
                if (current.savedId == 0L) {
                    saveRecord(built, input.value, favoriteOverride = true)
                } else {
                    val target = !current.isFavorite
                    qrRepository.setFavorite(current.savedId, target)
                    _state.update { it.copy(isFavorite = target) }
                }
            } finally {
                _state.update { it.copy(working = it.working - GeneratorAction.FAVORITE) }
            }
        }
    }

    fun saveToGallery() {
        val encoded = requireEncoded() ?: return
        runExport(GeneratorAction.SAVE, R.string.snack_export_failed) {
            exporter.savePng(encoded, _state.value.style, _state.value.builtTitle)
                .map { Unit }
                .onSuccess { event(R.string.snack_saved_gallery) }
        }
    }

    fun share() {
        val encoded = requireEncoded() ?: return
        runExport(GeneratorAction.SHARE, R.string.snack_share_failed) {
            exporter.prepareShareImage(encoded, _state.value.style, _state.value.builtTitle)
                .onSuccess { uri -> eventsChannel.send(GeneratorEvent.Share(uri)) }
                .map { Unit }
        }
    }

    fun exportPdf(layout: PdfLayout) {
        val encoded = requireEncoded() ?: return
        val state = _state.value
        runExport(GeneratorAction.PDF, R.string.snack_export_failed) {
            exporter.savePdf(
                encoded = encoded,
                style = state.style,
                title = state.builtTitle,
                type = state.type,
                layout = layout,
            )
                .map { Unit }
                .onSuccess { event(R.string.snack_pdf_saved) }
        }
    }

    fun copyPayload() {
        val built = requireBuilt() ?: return
        val sensitive = _state.value.type == QrType.WIFI
        val copied = clipboard.copy(built.payload, sensitive)
        event(
            when {
                !copied -> R.string.snack_copy_failed
                sensitive -> R.string.snack_copied_sensitive
                else -> R.string.snack_copied
            },
        )
    }

    private fun runExport(
        action: GeneratorAction,
        failureMessage: Int,
        block: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(working = it.working + action) }
            try {
                block().onFailure { event(failureMessage) }
            } finally {
                _state.update { it.copy(working = it.working - action) }
            }
        }
    }

    private fun requireEncoded(): EncodedQr? {
        val current = _state.value
        val encoded = current.encoded
        if (encoded != null) return encoded
        if (current.tooLong) {
            event(R.string.preview_too_long)
        } else if (current.encodeFailed) {
            event(R.string.preview_failed)
        } else {
            if (requireBuilt() != null) event(R.string.snack_preview_wait)
        }
        return null
    }

    private fun requireBuilt(): BuiltPayload? {
        val current = _state.value
        val built = PayloadBuilderRegistry.get(current.type).build(current.fields)
        if (built == null) {
            _state.update {
                val errors = PayloadBuilderRegistry.get(it.type).validate(it.fields)
                it.copy(
                    touched = it.touched + it.fields.keys + errors.keys,
                    errors = errors,
                )
            }
            event(R.string.snack_validation_failed)
        }
        return built
    }

    private suspend fun loadSaved(id: Long) {
        val saved = qrRepository.getById(id)
        if (saved == null) {
            _state.update {
                it.copy(savedId = 0L, draftLoaded = true)
            }
            event(R.string.snack_record_missing)
            return
        }
        if (initialDraft != null) {
            input.update { it.copy(type = saved.type) }
            _state.update {
                it.copy(
                    type = saved.type,
                    savedId = saved.id,
                    isFavorite = saved.isFavorite,
                    createdAt = saved.createdAt,
                    draftLoaded = true,
                )
            }
        } else {
            val style = saved.style.sanitized()
            input.value = GeneratorInput(saved.type, saved.fields, style)
            _state.update {
                it.copy(
                    type = saved.type,
                    fields = saved.fields,
                    errors = PayloadBuilderRegistry.get(saved.type).validate(saved.fields),
                    style = style,
                    savedId = saved.id,
                    isFavorite = saved.isFavorite,
                    createdAt = saved.createdAt,
                    draftLoaded = true,
                )
            }
        }
    }

    private suspend fun encodeAndRender(current: GeneratorInput) {
        val builder = PayloadBuilderRegistry.get(current.type)
        val fieldsErrors = builder.validate(current.fields)
        val built = builder.build(current.fields)
        if (built == null) {
            _state.update {
                it.copy(
                    errors = fieldsErrors,
                    payload = null,
                    builtTitle = "",
                    encoded = null,
                    preview = null,
                    isRendering = false,
                    tooLong = false,
                    encodeFailed = false,
                )
            }
            return
        }

        _state.update {
            it.copy(
                errors = emptyMap(),
                payload = built.payload,
                builtTitle = built.title,
                isRendering = true,
            )
        }

        val result = withContext(Dispatchers.Default) {
            encoder.encode(built.payload, current.style.effectiveErrorCorrection)
        }
        when (result) {
            EncodeResult.Empty -> _state.update {
                it.copy(preview = null, encoded = null, isRendering = false)
            }

            EncodeResult.TooLong -> _state.update {
                it.copy(
                    preview = null,
                    encoded = null,
                    isRendering = false,
                    tooLong = true,
                    encodeFailed = false,
                )
            }

            EncodeResult.Failure -> _state.update {
                it.copy(
                    preview = null,
                    encoded = null,
                    isRendering = false,
                    encodeFailed = true,
                    tooLong = false,
                )
            }

            is EncodeResult.Success -> {
                val render = withContext(Dispatchers.Default) {
                    QrPainter.render(
                        encoded = result.qr,
                        style = current.style,
                        targetSizePx = PREVIEW_SIZE,
                    )
                }
                _state.update {
                    it.copy(
                        encoded = result.qr,
                        preview = render.bitmap,
                        isRendering = false,
                        tooLong = false,
                        encodeFailed = render.bitmap == null,
                    )
                }
            }
        }
    }

    private suspend fun autosave(current: GeneratorInput) {
        if (!_state.value.draftLoaded) return
        val built = PayloadBuilderRegistry.get(current.type).build(current.fields) ?: return
        val currentState = _state.value
        if (currentState.savedId == 0L && !currentState.isFavorite) {
            val settings = settingsRepository.settings.first()
            if (!settings.saveHistory) return
        }
        saveRecord(built, current)
    }

    private suspend fun saveRecord(
        built: BuiltPayload,
        current: GeneratorInput,
        favoriteOverride: Boolean? = null,
    ): Long = saveMutex.withLock {
        val currentState = _state.value
        val now = System.currentTimeMillis()
        val existingId = currentState.savedId
        val favorite = favoriteOverride ?: currentState.isFavorite
        val createdAt = if (existingId == 0L) {
            now
        } else {
            currentState.createdAt.takeIf { it > 0L } ?: now
        }
        val record = SavedQr(
            id = existingId,
            type = current.type,
            title = built.title,
            payloadText = built.payload,
            fields = current.fields,
            style = current.style,
            createdAt = createdAt,
            updatedAt = now,
            isFavorite = favorite,
        )
        val savedId = qrRepository.save(record)
        _state.update {
            it.copy(
                savedId = if (existingId == 0L) savedId else existingId,
                createdAt = createdAt,
                isFavorite = favorite,
            )
        }
        savedId
    }

    private fun applyStyle(style: QrStyle) {
        val safe = style.sanitized()
        updateForm(fields = _state.value.fields, style = safe)
    }

    private fun updateForm(fields: Map<String, String>, style: QrStyle) {
        val type = _state.value.type
        input.value = GeneratorInput(type, fields, style)
        _state.update {
            it.copy(
                fields = fields,
                style = style,
                errors = PayloadBuilderRegistry.get(type).validate(fields),
            )
        }
        savedStateHandle[KEY_FIELDS] = AppJson.encodeToString(fieldsSerializer, fields)
        savedStateHandle[KEY_STYLE] = AppJson.encodeToString(QrStyle.serializer(), style)
    }

    private fun readDraft(): Pair<Map<String, String>, QrStyle>? {
        val fieldsJson = savedStateHandle.get<String>(KEY_FIELDS) ?: return null
        return runCatching {
            val fields = AppJson.decodeFromString(fieldsSerializer, fieldsJson)
            val style = savedStateHandle.get<String>(KEY_STYLE)
                ?.let { AppJson.decodeFromString(QrStyle.serializer(), it) }
                ?: QrStyle()
            fields to style.sanitized()
        }.getOrNull()
    }

    private fun event(@StringRes messageRes: Int) {
        eventsChannel.trySend(GeneratorEvent.Snackbar(messageRes))
    }

    private companion object {
        const val RENDER_DEBOUNCE_MS = 250L
        const val AUTOSAVE_DEBOUNCE_MS = 700L
        const val PREVIEW_SIZE = 640
        const val KEY_FIELDS = "draft_fields"
        const val KEY_STYLE = "draft_style"
    }
}
