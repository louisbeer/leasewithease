package com.beer.leasewithease.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * A longer drive the user already knows about, booked against a contract's allowance before it is
 * driven.
 *
 * While the [date] is still ahead, the [kilometers] are held back from the contract's budget so the
 * everyday balance already answers "do I still get by *including* that trip?". Once the date has
 * passed the reservation is dropped again, because by then the kilometers show up in the odometer
 * reading instead. See [com.beer.leasewithease.domain.usecase.PlannedTripSchedule].
 */
@Entity(
    tableName = "planned_trip",
    foreignKeys = [
        ForeignKey(
            entity = Contract::class,
            parentColumns = ["id"],
            childColumns = ["contractId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contractId")]
)
data class PlannedTrip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contractId: Long,
    val kilometers: Int,
    val date: Date,
    val label: String? = null
)
