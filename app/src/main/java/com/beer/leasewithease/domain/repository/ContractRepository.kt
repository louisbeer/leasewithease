package com.beer.leasewithease.domain.repository

import com.beer.leasewithease.data.local.ContractDao
import com.beer.leasewithease.data.local.PlannedTripDao
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractWithTrips
import com.beer.leasewithease.domain.model.PlannedTrip
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class ContractRepository @Inject constructor(
    private val contractDao: ContractDao,
    private val plannedTripDao: PlannedTripDao
) {

    fun getAllContractsWithTrips(): Flow<List<ContractWithTrips>> =
        contractDao.getAllContractsWithTrips()

    suspend fun getContractById(id: Int): Contract? = contractDao.getContractById(id)

    suspend fun getContractWithTripsById(id: Long): ContractWithTrips? =
        contractDao.getContractWithTripsById(id)

    suspend fun insertContract(contract: Contract) {
        contractDao.insert(contract)
    }

    suspend fun updateContract(contract: Contract) {
        contractDao.update(contract)
    }

    /**
     * Records a fresh odometer reading.
     *
     * The only path that should write [Contract.vehicleMileage] from a reading the user took, as
     * opposed to the edit form carrying the existing value through: it stamps
     * [Contract.lastMileageUpdateAt], which is what clears the "a planned trip has already
     * happened" reminder.
     */
    suspend fun updateMileage(contract: Contract, newMileage: Int, now: Date = Date()) {
        contractDao.update(contract.copy(vehicleMileage = newMileage, lastMileageUpdateAt = now))
    }

    suspend fun deleteContract(contract: Contract) {
        contractDao.delete(contract)
    }

    fun getTripsForContract(contractId: Long): Flow<List<PlannedTrip>> =
        plannedTripDao.getTripsForContract(contractId)

    suspend fun insertTrip(trip: PlannedTrip) {
        plannedTripDao.insert(trip)
    }

    suspend fun updateTrip(trip: PlannedTrip) {
        plannedTripDao.update(trip)
    }

    suspend fun deleteTrip(trip: PlannedTrip) {
        plannedTripDao.delete(trip)
    }
}
