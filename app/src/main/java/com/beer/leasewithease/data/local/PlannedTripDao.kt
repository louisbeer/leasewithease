package com.beer.leasewithease.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beer.leasewithease.domain.model.PlannedTrip
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedTripDao {

    @Query("SELECT * FROM planned_trip WHERE contractId = :contractId ORDER BY date ASC")
    fun getTripsForContract(contractId: Long): Flow<List<PlannedTrip>>

    @Insert
    suspend fun insert(trip: PlannedTrip)

    @Update
    suspend fun update(trip: PlannedTrip)

    @Delete
    suspend fun delete(trip: PlannedTrip)
}
