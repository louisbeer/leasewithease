package com.beer.leasewithease.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beer.leasewithease.domain.model.Contract
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractDao {

    @Query("SELECT * FROM contract")
    fun getAllContracts(): Flow<List<Contract>>

    @Query("SELECT * FROM contract WHERE id = :id")
    suspend fun getContractById(id: Int): Contract?

    @Insert
    suspend fun insert(contract: Contract)

    @Update
    suspend fun update(contract: Contract)

    @Delete
    suspend fun delete(contract: Contract)
}
