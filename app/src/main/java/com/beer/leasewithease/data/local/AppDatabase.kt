package com.beer.leasewithease.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beer.leasewithease.domain.model.Contract

@Database(entities = [Contract::class], version = 3)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contractDao(): ContractDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE contract ADD COLUMN startDate INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE contract ADD COLUMN durationInMonths INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE contract ADD COLUMN includedKilometers INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE contract ADD COLUMN costPerExtraKilometer REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE contract ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE contract ADD COLUMN mileageAtContractStart INTEGER NOT NULL DEFAULT 0")
    }
}
