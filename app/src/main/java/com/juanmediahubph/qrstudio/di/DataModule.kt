package com.juanmediahubph.qrstudio.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.juanmediahubph.qrstudio.data.db.QrDao
import com.juanmediahubph.qrstudio.data.db.QrDatabase
import com.juanmediahubph.qrstudio.data.db.QrMigrations
import com.juanmediahubph.qrstudio.data.repository.LogoRepositoryImpl
import com.juanmediahubph.qrstudio.data.repository.QrRepositoryImpl
import com.juanmediahubph.qrstudio.data.repository.SettingsRepositoryImpl
import com.juanmediahubph.qrstudio.data.clipboard.ClipboardWriter
import com.juanmediahubph.qrstudio.data.export.QrExporterImpl
import com.juanmediahubph.qrstudio.domain.export.QrExporter
import com.juanmediahubph.qrstudio.domain.repository.LogoRepository
import com.juanmediahubph.qrstudio.domain.repository.PayloadClipboard
import com.juanmediahubph.qrstudio.domain.repository.QrRepository
import com.juanmediahubph.qrstudio.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindQrRepository(impl: QrRepositoryImpl): QrRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLogoRepository(impl: LogoRepositoryImpl): LogoRepository

    @Binds
    @Singleton
    abstract fun bindPayloadClipboard(impl: ClipboardWriter): PayloadClipboard

    @Binds
    @Singleton
    abstract fun bindQrExporter(impl: QrExporterImpl): QrExporter
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QrDatabase =
        Room.databaseBuilder(context, QrDatabase::class.java, QrDatabase.NAME)
            .addMigrations(*QrMigrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideQrDao(database: QrDatabase): QrDao = database.qrDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("settings") },
    )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
