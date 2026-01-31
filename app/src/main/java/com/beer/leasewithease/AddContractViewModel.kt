package com.beer.leasewithease

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractType
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddContractViewModel @Inject constructor(
    private val repository: ContractRepository
) : ViewModel() {

    fun saveContract(
        vehicleName: String,
        vehicleType: VehicleType,
        contractType: ContractType,
        startDate: Date,
        durationInMonths: Int,
        includedKilometers: Int,
        costPerExtraKilometer: Float,
        isRecurring: Boolean,
        vehicleMileage: Int,
        mileageAtContractStart: Int
    ) {
        viewModelScope.launch {
            val contract = Contract(
                vehicleName = vehicleName,
                vehicleType = vehicleType,
                contractType = contractType,
                startDate = startDate,
                durationInMonths = durationInMonths,
                includedKilometers = includedKilometers,
                costPerExtraKilometer = costPerExtraKilometer,
                isRecurring = isRecurring,
                vehicleMileage = vehicleMileage,
                mileageAtContractStart = mileageAtContractStart
            )
            repository.insertContract(contract)
        }
    }
}
