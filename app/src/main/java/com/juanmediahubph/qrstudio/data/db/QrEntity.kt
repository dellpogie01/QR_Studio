package com.juanmediahubph.qrstudio.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "qr_codes",
    indices = [
        Index("createdAt"),
        Index("updatedAt"),
        Index("type"),
        Index("isFavorite"),
    ],
)
data class QrEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val payloadText: String,
    val formFieldsJson: String,
    val styleJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
)
