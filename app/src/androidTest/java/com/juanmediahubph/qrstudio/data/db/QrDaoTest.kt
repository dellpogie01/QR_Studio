package com.juanmediahubph.qrstudio.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrDaoTest {

    private lateinit var database: QrDatabase
    private lateinit var dao: QrDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.qrDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsSearchesFiltersAndDeletes() = runBlocking {
        val urlId = dao.insert(entity(type = "URL", title = "example.com", updatedAt = 10L))
        dao.insert(entity(type = "WIFI", title = "Home network", updatedAt = 20L, favorite = true))

        assertEquals(2, dao.count())

        val newestFirst = dao.observe("", false, null).first()
        assertEquals(listOf("Home network", "example.com"), newestFirst.map { it.title })

        assertEquals(1, dao.observe("home", false, null).first().size)
        assertEquals(1, dao.observe("", true, null).first().size)
        assertEquals(1, dao.observe("", false, "URL").first().size)

        dao.setFavorite(urlId, true, 30L)
        assertEquals(2, dao.observe("", true, null).first().size)

        dao.deleteByIds(listOf(urlId))
        assertEquals(1, dao.count())
        dao.clear()
        assertEquals(0, dao.count())
    }

    private fun entity(
        type: String,
        title: String,
        updatedAt: Long,
        favorite: Boolean = false,
    ) = QrEntity(
        type = type,
        title = title,
        payloadText = title,
        formFieldsJson = "{}",
        styleJson = "{}",
        createdAt = updatedAt,
        updatedAt = updatedAt,
        isFavorite = favorite,
    )
}
