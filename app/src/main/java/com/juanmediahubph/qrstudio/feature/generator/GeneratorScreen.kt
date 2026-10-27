@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.juanmediahubph.qrstudio.feature.generator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.core.ui.components.QrImage
import com.juanmediahubph.qrstudio.core.ui.labelRes
import com.juanmediahubph.qrstudio.core.util.ContrastCalculator
import com.juanmediahubph.qrstudio.domain.model.PdfLayout
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.qr.QrCapacity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun GeneratorScreen(
    onBack: () -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCustomize by rememberSaveable { mutableStateOf(false) }
    var showPdfDialog by rememberSaveable { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val shouldSave = pendingSave
        pendingSave = false
        if (granted && shouldSave) {
            viewModel.saveToGallery()
        } else if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.snack_permission_denied))
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GeneratorEvent.Snackbar ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))

                is GeneratorEvent.Share ->
                    shareImage(context, event.uri, snackbarHostState, scope)
            }
        }
    }

    fun saveWithPermission() {
        val needsPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingSave = true
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.saveToGallery()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.generator_heading,
                            stringResource(state.type.labelRes()),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        enabled = GeneratorAction.FAVORITE !in state.working,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isFavorite) {
                                    R.drawable.ic_action_favorite_filled
                                } else {
                                    R.drawable.ic_action_favorite
                                },
                            ),
                            contentDescription = stringResource(
                                if (state.isFavorite) R.string.unfavorite else R.string.favorite,
                            ),
                            tint = if (state.isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        DetailsSection(state = state, onFieldChange = viewModel::onFieldChange)
                        Spacer(Modifier.height(24.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            val previewSide = minOf(maxWidth, maxHeight).coerceAtLeast(120.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PreviewPanel(state = state, previewSide = previewSide)
                            }
                        }
                        ActionPanel(
                            state = state,
                            onSave = ::saveWithPermission,
                            onShare = viewModel::share,
                            onCopy = viewModel::copyPayload,
                            onPdf = { showPdfDialog = true },
                        )
                        OutlinedButton(
                            onClick = { showCustomize = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_palette),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.customize),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PreviewPanel(state)
                    ActionPanel(
                        state = state,
                        onSave = ::saveWithPermission,
                        onShare = viewModel::share,
                        onCopy = viewModel::copyPayload,
                        onPdf = { showPdfDialog = true },
                    )
                    OutlinedButton(
                        onClick = { showCustomize = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_palette),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.customize),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    DetailsSection(state = state, onFieldChange = viewModel::onFieldChange)
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }

    if (showCustomize) {
        CustomizeSheet(
            state = state,
            onDismiss = { showCustomize = false },
            onStyleChange = viewModel::onStyleChange,
            onImportLogo = viewModel::importLogo,
            onRemoveLogo = viewModel::removeLogo,
        )
    }

    if (showPdfDialog) {
        PdfLayoutDialog(
            onDismiss = { showPdfDialog = false },
            onExport = { layout ->
                showPdfDialog = false
                viewModel.exportPdf(layout)
            },
        )
    }
}

@Composable
private fun DetailsSection(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.details_section),
            style = MaterialTheme.typography.titleMedium,
        )
        GeneratorForm(state = state, onFieldChange = onFieldChange)
    }
}

@Composable
private fun PreviewPanel(state: GeneratorUiState, previewSide: Dp? = null) {
    val contrast = remember(state.style.foreground, state.style.background) {
        ContrastCalculator.evaluate(state.style.foreground, state.style.background)
    }
    val hasLogo = state.style.logoPath != null
    val payload = state.payload
    val level = state.style.effectiveErrorCorrection
    val allBlank = state.fields.values.all { it.isBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val cardModifier = if (previewSide != null) {
            Modifier.size(previewSide)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        }
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = cardModifier,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (previewSide != null) 16.dp else 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = state.preview,
                    animationSpec = tween(durationMillis = 220),
                    label = "qr-preview",
                ) { bitmap ->
                    if (bitmap != null) {
                        QrImage(
                            bitmap = bitmap,
                            contentDescription = stringResource(R.string.preview_content_desc),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PreviewPlaceholder(
                            isRendering = state.isRendering,
                            allBlank = allBlank,
                        )
                    }
                }
            }
        }

        when {
            state.tooLong -> PreviewMessage(stringResource(R.string.preview_too_long), isError = true)
            state.encodeFailed -> PreviewMessage(stringResource(R.string.preview_failed), isError = true)
            contrast.isLowContrast ->
                PreviewMessage(stringResource(R.string.contrast_warning), isError = true)

            contrast.isInverted ->
                PreviewMessage(stringResource(R.string.inverted_warning), isError = false)
        }
        if (hasLogo) {
            PreviewMessage(stringResource(R.string.logo_forced_ec), isError = false)
        }
        if (payload != null && state.type != QrType.TEXT) {
            val over = QrCapacity.isOverCapacity(payload, level)
            val near = !over && QrCapacity.isNearCapacity(payload, level)
            when {
                over -> PreviewMessage(stringResource(R.string.capacity_over), isError = true)
                near -> PreviewMessage(stringResource(R.string.capacity_near), isError = false)
            }
        }
    }
}

@Composable
private fun PreviewPlaceholder(isRendering: Boolean, allBlank: Boolean) {
    if (isRendering) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Text(
                text = stringResource(R.string.preview_rendering),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_empty_qr),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = stringResource(
                    if (allBlank) R.string.preview_placeholder else R.string.preview_invalid,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PreviewMessage(text: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_action_info),
            contentDescription = null,
            tint = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ActionPanel(
    state: GeneratorUiState,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onPdf: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            labelRes = R.string.action_save,
            iconRes = R.drawable.ic_action_save,
            loading = GeneratorAction.SAVE in state.working,
            onClick = onSave,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            labelRes = R.string.action_share,
            iconRes = R.drawable.ic_action_share,
            loading = GeneratorAction.SHARE in state.working,
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            labelRes = R.string.action_copy,
            iconRes = R.drawable.ic_action_copy,
            loading = false,
            onClick = onCopy,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            labelRes = R.string.action_pdf,
            iconRes = R.drawable.ic_action_pdf,
            loading = GeneratorAction.PDF in state.working,
            onClick = onPdf,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = !loading,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PdfLayoutDialog(
    onDismiss: () -> Unit,
    onExport: (PdfLayout) -> Unit,
) {
    var layout by rememberSaveable { mutableStateOf(PdfLayout.A4) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.pdf_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PdfOption(
                    title = stringResource(R.string.pdf_a4),
                    description = stringResource(R.string.pdf_a4_desc),
                    selected = layout == PdfLayout.A4,
                    onClick = { layout = PdfLayout.A4 },
                )
                PdfOption(
                    title = stringResource(R.string.pdf_sticker),
                    description = stringResource(R.string.pdf_sticker_desc),
                    selected = layout == PdfLayout.STICKER,
                    onClick = { layout = PdfLayout.STICKER },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(layout) }) {
                Text(stringResource(R.string.action_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun PdfOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun shareImage(
    context: Context,
    uri: Uri,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser_title))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(chooser) }.onFailure {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.snack_share_failed))
        }
    }
}
