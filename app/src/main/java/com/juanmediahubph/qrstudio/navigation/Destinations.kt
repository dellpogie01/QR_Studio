package com.juanmediahubph.qrstudio.navigation

import com.juanmediahubph.qrstudio.domain.model.QrType

object Destinations {
    const val CREATE = "create"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val GENERATOR_ROUTE = "generator/{type}?id={id}"
    const val TYPE_ARG = "type"
    const val ID_ARG = "id"

    val topLevelRoutes = listOf(CREATE, HISTORY, SETTINGS)

    fun generator(type: QrType, id: Long = 0L): String {
        val base = "generator/${type.name}"
        return if (id > 0L) "$base?id=$id" else base
    }
}
