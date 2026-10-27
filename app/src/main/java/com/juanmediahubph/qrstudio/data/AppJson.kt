package com.juanmediahubph.qrstudio.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    serializersModule = SerializersModule {}
}
