package com.beer.leasewithease.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.PlannedTrip

@Database(entities = [Contract::class, PlannedTrip::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contractDao(): ContractDao
    abstract fun plannedTripDao(): PlannedTripDao
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

// Planned trips, plus the odometer timestamp that tells whether a trip already driven has been
// entered yet. The DDL has to match what Room generates for the entities verbatim, identifier
// quoting included, or Room rejects the schema on open.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE contract ADD COLUMN lastMileageUpdateAt INTEGER")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `planned_trip` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`contractId` INTEGER NOT NULL, " +
                "`kilometers` INTEGER NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`label` TEXT, " +
                "FOREIGN KEY(`contractId`) REFERENCES `Contract`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_planned_trip_contractId` " +
                "ON `planned_trip` (`contractId`)"
        )
    }
}
