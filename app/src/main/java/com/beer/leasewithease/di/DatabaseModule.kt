package com.beer.leasewithease.di

import android.content.Context
import androidx.room.Room
import com.beer.leasewithease.data.local.AppDatabase
import com.beer.leasewithease.data.local.ContractDao
import com.beer.leasewithease.data.local.MIGRATION_1_2
import com.beer.leasewithease.data.local.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "lease_with_ease_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideContractDao(appDatabase: AppDatabase): ContractDao {
        return appDatabase.contractDao()
    }
}