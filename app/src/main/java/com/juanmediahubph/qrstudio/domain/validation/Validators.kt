package com.juanmediahubph.qrstudio.domain.validation

import java.net.URI
import java.net.URISyntaxException

object UrlNormalizer {
    private val schemePattern = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")

    /**
     * Returns a browsable http(s) URL, prefixing `https://` when no scheme was supplied.
     * Returns null for anything that is not a plausible web address.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val candidate = if (schemePattern.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val uri = try {
            URI(candidate)
        } catch (_: URISyntaxException) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        if (!isValidHost(host)) return null
        if (uri.port < -1 || uri.port > 65535) return null
        return candidate
    }

    fun hostOf(normalizedUrl: String): String? = try {
        URI(normalizedUrl).host?.removePrefix("www.")
    } catch (_: URISyntaxException) {
        null
    }

    private fun isValidHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        if (host.startsWith("[") && host.endsWith("]")) return true
        val labels = host.split('.')
        if (labels.size < 2) return false
        if (labels.any { it.isEmpty() || it.length > 63 }) return false
        if (labels.all { label -> label.all(Char::isDigit) }) {
            return labels.size == 4 && labels.all { label ->
                label.toIntOrNull()?.let { it in 0..255 } == true
            }
        }
        val tld = labels.last()
        if (tld.any { !it.isLetter() }) return false
        return labels.all { label ->
            label.first().isLetterOrDigit() &&
                label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        }
    }
}

object EmailAddress {
    private val pattern = Regex(
        "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@" +
            "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
            "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$",
    )

    fun isValid(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.length in 3..254 && pattern.matches(trimmed)
    }

    fun normalize(raw: String): String = raw.trim()
}

object PhoneNumber {
    private val removable = setOf(' ', '-', '(', ')', '.', '/')

    /** Normalizes display formatting into a dialable string such as `+14155550123`. */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val cleaned = trimmed.filterNot { it in removable }
        if (cleaned.isEmpty()) return null
        if (cleaned.any { !it.isDigit() && it != '+' }) return null
        if (cleaned.count { it == '+' } > 1) return null
        if (cleaned.contains('+') && !cleaned.startsWith('+')) return null
        val digitCount = cleaned.count(Char::isDigit)
        if (digitCount !in 3..15) return null
        return cleaned
    }
}
