package com.pangea.oriontek.data.di

import android.content.Context
import androidx.room.Room
import com.pangea.oriontek.data.local.dao.ClientDao
import com.pangea.oriontek.data.local.database.ClientDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ClientDatabase = Room.databaseBuilder(
        context,
        ClientDatabase::class.java,
        "oriontek_db"
    )
        .fallbackToDestructiveMigration(false)
        .build()

    @Provides
    fun provideClientDao(db: ClientDatabase): ClientDao = db.clientDao()
}