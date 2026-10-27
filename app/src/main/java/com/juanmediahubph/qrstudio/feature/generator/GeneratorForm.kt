@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.juanmediahubph.qrstudio.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.payload.EmailPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.FormError
import com.juanmediahubph.qrstudio.domain.payload.LocationPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.PhonePayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.SmsPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.TextPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.UrlPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.VCardPayloadBuilder
import com.juanmediahubph.qrstudio.domain.payload.WifiPayloadBuilder
import com.juanmediahubph.qrstudio.domain.qr.QrCapacity
import androidx.annotation.StringRes

@Composable
fun GeneratorForm(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (state.type) {
            QrType.WIFI -> WifiFields(state, onFieldChange)
            QrType.VCARD -> VCardFields(state, onFieldChange)
            QrType.URL -> UrlFields(state, onFieldChange)
            QrType.TEXT -> TextFields(state, onFieldChange)
            QrType.EMAIL -> EmailFields(state, onFieldChange)
            QrType.PHONE -> PhoneFields(state, onFieldChange)
            QrType.SMS -> SmsFields(state, onFieldChange)
            QrType.LOCATION -> LocationFields(state, onFieldChange)
        }
    }
}

@Composable
private fun WifiFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val security = WifiPayloadBuilder.securityFrom(state.fields)
    val hidden = state.fields[WifiPayloadBuilder.HIDDEN].toBoolean()

    GeneratorTextField(
        value = state.field(WifiPayloadBuilder.SSID),
        onValueChange = { onFieldChange(WifiPayloadBuilder.SSID, it) },
        label = stringResource(R.string.field_ssid),
        error = state.errorFor(WifiPayloadBuilder.SSID),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )

    Text(
        text = stringResource(R.string.field_security),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WifiPayloadBuilder.Security.entries.forEach { option ->
            FilterChip(
                selected = security == option,
                onClick = { onFieldChange(WifiPayloadBuilder.SECURITY, option.name) },
                label = {
                    Text(
                        stringResource(
                            when (option) {
                                WifiPayloadBuilder.Security.WPA -> R.string.security_wpa
                                WifiPayloadBuilder.Security.WEP -> R.string.security_wep
                                WifiPayloadBuilder.Security.NONE -> R.string.security_none
                            },
                        ),
                    )
                },
            )
        }
    }

    if (security == WifiPayloadBuilder.Security.NONE) {
        Text(
            text = stringResource(R.string.password_not_needed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        GeneratorTextField(
            value = state.field(WifiPayloadBuilder.PASSWORD),
            onValueChange = { onFieldChange(WifiPayloadBuilder.PASSWORD, it) },
            label = stringResource(R.string.field_password),
            error = state.errorFor(WifiPayloadBuilder.PASSWORD),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) {
                                R.drawable.ic_action_visibility_off
                            } else {
                                R.drawable.ic_action_visibility
                            },
                        ),
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.hide_password else R.string.show_password,
                        ),
                    )
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.field_hidden_network),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = hidden,
            onCheckedChange = { onFieldChange(WifiPayloadBuilder.HIDDEN, it.toString()) },
        )
    }
}

@Composable
private fun VCardFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.NAME),
        onValueChange = { onFieldChange(VCardPayloadBuilder.NAME, it) },
        label = stringResource(R.string.field_name),
        error = state.errorFor(VCardPayloadBuilder.NAME),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.ORGANIZATION),
        onValueChange = { onFieldChange(VCardPayloadBuilder.ORGANIZATION, it) },
        label = stringResource(R.string.field_organization),
        error = state.errorFor(VCardPayloadBuilder.ORGANIZATION),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.JOB_TITLE),
        onValueChange = { onFieldChange(VCardPayloadBuilder.JOB_TITLE, it) },
        label = stringResource(R.string.field_job_title),
        error = state.errorFor(VCardPayloadBuilder.JOB_TITLE),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.PHONE),
        onValueChange = { onFieldChange(VCardPayloadBuilder.PHONE, it) },
        label = stringResource(R.string.field_phone),
        error = state.errorFor(VCardPayloadBuilder.PHONE),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.EMAIL),
        onValueChange = { onFieldChange(VCardPayloadBuilder.EMAIL, it) },
        label = stringResource(R.string.field_email),
        error = state.errorFor(VCardPayloadBuilder.EMAIL),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.URL),
        onValueChange = { onFieldChange(VCardPayloadBuilder.URL, it) },
        label = stringResource(R.string.field_website),
        error = state.errorFor(VCardPayloadBuilder.URL),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.ADDRESS),
        onValueChange = { onFieldChange(VCardPayloadBuilder.ADDRESS, it) },
        label = stringResource(R.string.field_address),
        error = state.errorFor(VCardPayloadBuilder.ADDRESS),
        minLines = 2,
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
    GeneratorTextField(
        value = state.field(VCardPayloadBuilder.NOTE),
        onValueChange = { onFieldChange(VCardPayloadBuilder.NOTE, it) },
        label = stringResource(R.string.field_note),
        error = state.errorFor(VCardPayloadBuilder.NOTE),
        minLines = 2,
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun UrlFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    GeneratorTextField(
        value = state.field(UrlPayloadBuilder.URL),
        onValueChange = { onFieldChange(UrlPayloadBuilder.URL, it) },
        label = stringResource(R.string.field_url),
        error = state.errorFor(UrlPayloadBuilder.URL),
        placeholder = stringResource(R.string.field_url_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
    )
}

@Composable
private fun TextFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    val text = state.field(TextPayloadBuilder.TEXT)
    val level = state.style.effectiveErrorCorrection
    val over = QrCapacity.isOverCapacity(text, level)
    val near = !over && QrCapacity.isNearCapacity(text, level)

    GeneratorTextField(
        value = text,
        onValueChange = { onFieldChange(TextPayloadBuilder.TEXT, it) },
        label = stringResource(R.string.field_text),
        error = state.errorFor(TextPayloadBuilder.TEXT),
        singleLine = false,
        minLines = 5,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        supportingText = pluralStringResource(R.plurals.char_count, text.length, text.length),
    )
    when {
        over -> Text(
            text = stringResource(R.string.capacity_over),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        near -> Text(
            text = stringResource(R.string.capacity_near),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun EmailFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    GeneratorTextField(
        value = state.field(EmailPayloadBuilder.TO),
        onValueChange = { onFieldChange(EmailPayloadBuilder.TO, it) },
        label = stringResource(R.string.field_email_to),
        error = state.errorFor(EmailPayloadBuilder.TO),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(EmailPayloadBuilder.SUBJECT),
        onValueChange = { onFieldChange(EmailPayloadBuilder.SUBJECT, it) },
        label = stringResource(R.string.field_subject),
        error = state.errorFor(EmailPayloadBuilder.SUBJECT),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(EmailPayloadBuilder.BODY),
        onValueChange = { onFieldChange(EmailPayloadBuilder.BODY, it) },
        label = stringResource(R.string.field_message),
        error = state.errorFor(EmailPayloadBuilder.BODY),
        singleLine = false,
        minLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun PhoneFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    GeneratorTextField(
        value = state.field(PhonePayloadBuilder.NUMBER),
        onValueChange = { onFieldChange(PhonePayloadBuilder.NUMBER, it) },
        label = stringResource(R.string.field_number),
        error = state.errorFor(PhonePayloadBuilder.NUMBER),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
    )
}

@Composable
private fun SmsFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    GeneratorTextField(
        value = state.field(SmsPayloadBuilder.NUMBER),
        onValueChange = { onFieldChange(SmsPayloadBuilder.NUMBER, it) },
        label = stringResource(R.string.field_number),
        error = state.errorFor(SmsPayloadBuilder.NUMBER),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
    )
    GeneratorTextField(
        value = state.field(SmsPayloadBuilder.MESSAGE),
        onValueChange = { onFieldChange(SmsPayloadBuilder.MESSAGE, it) },
        label = stringResource(R.string.field_sms_message),
        error = state.errorFor(SmsPayloadBuilder.MESSAGE),
        singleLine = false,
        minLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun LocationFields(
    state: GeneratorUiState,
    onFieldChange: (String, String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GeneratorTextField(
            value = state.field(LocationPayloadBuilder.LATITUDE),
            onValueChange = { onFieldChange(LocationPayloadBuilder.LATITUDE, it) },
            label = stringResource(R.string.field_latitude),
            error = state.errorFor(LocationPayloadBuilder.LATITUDE),
            placeholder = stringResource(R.string.field_lat_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        GeneratorTextField(
            value = state.field(LocationPayloadBuilder.LONGITUDE),
            onValueChange = { onFieldChange(LocationPayloadBuilder.LONGITUDE, it) },
            label = stringResource(R.string.field_longitude),
            error = state.errorFor(LocationPayloadBuilder.LONGITUDE),
            placeholder = stringResource(R.string.field_lng_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun GeneratorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: FormError?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(stringResource(error.messageRes())) }
            }

            supportingText != null -> {
                { Text(supportingText) }
            }

            else -> null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else 8,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
    )
}

internal fun GeneratorUiState.field(key: String): String = fields[key].orEmpty()

internal fun GeneratorUiState.errorFor(key: String): FormError? =
    errors[key]?.takeIf { key in touched }

@StringRes
internal fun FormError.messageRes(): Int = when (this) {
    FormError.REQUIRED -> R.string.err_required
    FormError.INVALID_URL -> R.string.err_invalid_url
    FormError.INVALID_EMAIL -> R.string.err_invalid_email
    FormError.INVALID_PHONE -> R.string.err_invalid_phone
    FormError.INVALID_LATITUDE -> R.string.err_invalid_lat
    FormError.INVALID_LONGITUDE -> R.string.err_invalid_lng
    FormError.SSID_TOO_LONG -> R.string.err_ssid_too_long
    FormError.NAME_OR_PHONE_REQUIRED -> R.string.err_missing_name_or_phone
}
