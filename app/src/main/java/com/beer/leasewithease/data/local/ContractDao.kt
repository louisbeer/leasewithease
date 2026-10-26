package com.beer.leasewithease.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractWithTrips
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractDao {

    @Query("SELECT * FROM contract")
    fun getAllContracts(): Flow<List<Contract>>

    @Transaction
    @Query("SELECT * FROM contract")
    fun getAllContractsWithTrips(): Flow<List<ContractWithTrips>>

    @Query("SELECT * FROM contract WHERE id = :id")
    suspend fun getContractById(id: Int): Contract?

    @Transaction
    @Query("SELECT * FROM contract WHERE id = :id")
    suspend fun getContractWithTripsById(id: Long): ContractWithTrips?

    @Insert
    suspend fun insert(contract: Contract)

    @Update
    suspend fun update(contract: Contract)

    @Delete
    suspend fun delete(contract: Contract)
}
