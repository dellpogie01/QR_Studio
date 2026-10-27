package com.juanmediahubph.qrstudio.feature.generator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.core.theme.QrPalette
import com.juanmediahubph.qrstudio.core.ui.parseHexColor
import com.juanmediahubph.qrstudio.core.ui.toComposeColor
import com.juanmediahubph.qrstudio.core.ui.toHexString
import com.juanmediahubph.qrstudio.domain.model.EcLevel
import com.juanmediahubph.qrstudio.domain.model.ModuleStyle
import com.juanmediahubph.qrstudio.domain.model.QrSize
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    state: GeneratorUiState,
    onDismiss: () -> Unit,
    onStyleChange: ((QrStyle) -> QrStyle) -> Unit,
    onImportLogo: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val style = state.style
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImportLogo(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = stringResource(R.string.customize_title),
                style = MaterialTheme.typography.titleLarge,
            )

            SectionLabel(stringResource(R.string.customize_style))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.label_module_style),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleStyle.entries.forEach { module ->
                        FilterChip(
                            selected = style.moduleStyle == module,
                            onClick = { onStyleChange { it.copy(moduleStyle = module) } },
                            label = {
                                Text(
                                    stringResource(
                                        when (module) {
                                            ModuleStyle.SQUARE -> R.string.module_square
                                            ModuleStyle.ROUNDED -> R.string.module_rounded
                                            ModuleStyle.DOTS -> R.string.module_dots
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.label_size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QrSize.entries.forEach { size ->
                        FilterChip(
                            selected = style.size == size,
                            onClick = { onStyleChange { it.copy(size = size) } },
                            label = { Text(stringResource(R.string.size_pixels, size.px)) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.label_error_correction),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EcLevel.entries.forEach { level ->
                        FilterChip(
                            selected = style.effectiveErrorCorrection == level,
                            enabled = style.logoPath == null,
                            onClick = { onStyleChange { it.copy(errorCorrection = level) } },
                            label = {
                                Text(
                                    stringResource(
                                        when (level) {
                                            EcLevel.L -> R.string.ec_low
                                            EcLevel.M -> R.string.ec_medium
                                            EcLevel.Q -> R.string.ec_quartile
                                            EcLevel.H -> R.string.ec_high
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                Text(
                    text = stringResource(
                        if (style.logoPath != null) R.string.logo_forced_ec else R.string.ec_hint,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel(stringResource(R.string.customize_colors))
            ColorSelector(
                label = stringResource(R.string.label_foreground),
                selected = style.foreground,
                onSelect = { color -> onStyleChange { it.copy(foreground = color) } },
            )
            ColorSelector(
                label = stringResource(R.string.label_background),
                selected = style.background,
                onSelect = { color -> onStyleChange { it.copy(background = color) } },
            )

            SectionLabel(stringResource(R.string.customize_advanced))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.label_quiet_zone),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.quiet_zone_value,
                            style.quietZone,
                            style.quietZone,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = style.quietZone.toFloat(),
                    onValueChange = { value ->
                        onStyleChange { it.copy(quietZone = value.roundToInt()) }
                    },
                    valueRange = QrStyle.MIN_QUIET_ZONE.toFloat()..QrStyle.MAX_QUIET_ZONE.toFloat(),
                    steps = QrStyle.MAX_QUIET_ZONE - QrStyle.MIN_QUIET_ZONE - 1,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.label_logo),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LogoPreview(path = style.logoPath)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(onClick = { logoPicker.launch("image/*") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_image_add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.logo_add),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        if (style.logoPath != null) {
                            TextButton(onClick = onRemoveLogo) {
                                Text(stringResource(R.string.logo_remove))
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.logo_size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.logo_size_value,
                                (style.logoScale * 100).roundToInt(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = style.logoScale,
                        onValueChange = { value -> onStyleChange { it.copy(logoScale = value) } },
                        enabled = style.logoPath != null,
                        valueRange = QrStyle.MIN_LOGO_SCALE..QrStyle.MAX_LOGO_SCALE,
                        steps = 7,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ColorSelector(
    label: String,
    selected: Long,
    onSelect: (Long) -> Unit,
) {
    var hexText by rememberSaveable { mutableStateOf(selected.toHexString()) }
    LaunchedEffect(selected) {
        if (parseHexColor(hexText) != selected) hexText = selected.toHexString()
    }
    val parsed = parseHexColor(hexText)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(QrPalette.swatches) { color ->
                val isSelected = color == selected
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(color) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color.toComposeColor())
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
        OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
                hexText = input
                parseHexColor(input)?.let(onSelect)
            },
            label = { Text(stringResource(R.string.label_hex)) },
            singleLine = true,
            isError = parsed == null,
            supportingText = if (parsed == null) {
                { Text(stringResource(R.string.color_hex_invalid)) }
            } else {
                null
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Start,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LogoPreview(path: String?) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) }
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = stringResource(R.string.logo_preview_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_action_image_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
