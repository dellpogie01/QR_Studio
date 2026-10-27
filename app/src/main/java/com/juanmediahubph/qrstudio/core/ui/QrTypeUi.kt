package com.juanmediahubph.qrstudio.core.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.domain.model.QrType

@StringRes
fun QrType.labelRes(): Int = when (this) {
    QrType.WIFI -> R.string.type_wifi
    QrType.VCARD -> R.string.type_vcard
    QrType.URL -> R.string.type_url
    QrType.TEXT -> R.string.type_text
    QrType.EMAIL -> R.string.type_email
    QrType.PHONE -> R.string.type_phone
    QrType.SMS -> R.string.type_sms
    QrType.LOCATION -> R.string.type_location
}

@StringRes
fun QrType.descriptionRes(): Int = when (this) {
    QrType.WIFI -> R.string.type_wifi_desc
    QrType.VCARD -> R.string.type_vcard_desc
    QrType.URL -> R.string.type_url_desc
    QrType.TEXT -> R.string.type_text_desc
    QrType.EMAIL -> R.string.type_email_desc
    QrType.PHONE -> R.string.type_phone_desc
    QrType.SMS -> R.string.type_sms_desc
    QrType.LOCATION -> R.string.type_location_desc
}

@DrawableRes
fun QrType.iconRes(): Int = when (this) {
    QrType.WIFI -> R.drawable.ic_type_wifi
    QrType.VCARD -> R.drawable.ic_type_contact
    QrType.URL -> R.drawable.ic_type_url
    QrType.TEXT -> R.drawable.ic_type_text
    QrType.EMAIL -> R.drawable.ic_type_email
    QrType.PHONE -> R.drawable.ic_type_phone
    QrType.SMS -> R.drawable.ic_type_sms
    QrType.LOCATION -> R.drawable.ic_type_location
}
