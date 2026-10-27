package com.juanmediahubph.qrstudio.domain.repository

interface PayloadClipboard {
    fun copy(text: String, sensitive: Boolean): Boolean
}
