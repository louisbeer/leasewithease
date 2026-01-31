package com.beer.leasewithease.domain.repository

import com.beer.leasewithease.data.local.ContractDao
import com.beer.leasewithease.domain.model.Contract
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContractRepository @Inject constructor(private val contractDao: ContractDao) {

    fun getAllContracts(): Flow<List<Contract>> = contractDao.getAllContracts()

    suspend fun insertContract(contract: Contract) {
        contractDao.insert(contract)
    }

    suspend fun updateContract(contract: Contract) {
        contractDao.update(contract)
    }

    suspend fun deleteContract(contract: Contract) {
        contractDao.delete(contract)
    }
}
