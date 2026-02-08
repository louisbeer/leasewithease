package com.beer.leasewithease

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractType
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class EditContractViewModel @Inject constructor(
    private val contractRepository: ContractRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _contract = MutableStateFlow<Contract?>(null)
    val contract = _contract.asStateFlow()

    init {
        viewModelScope.launch {
            val contractId = savedStateHandle.get<Int>("contractId")
            if (contractId != null) {
                _contract.value = contractRepository.getContractById(contractId)
            }
        }
    }

    fun updateContract(
        vehicleName: String,
        vehicleMileage: Int,
        mileageAtContractStart: Int,
        vehicleType: VehicleType,
        contractType: ContractType,
        startDate: Date,
        durationInMonths: Int,
        includedKilometers: Int,
        costPerExtraKilometer: Float,
        isRecurring: Boolean,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentContract = _contract.value
                if (currentContract != null) {
                    val updatedContract = currentContract.copy(
                        vehicleName = vehicleName,
                        vehicleMileage = vehicleMileage,
                        mileageAtContractStart = mileageAtContractStart,
                        vehicleType = vehicleType,
                        contractType = contractType,
                        startDate = startDate,
                        durationInMonths = durationInMonths,
                        includedKilometers = includedKilometers,
                        costPerExtraKilometer = costPerExtraKilometer,
                        isRecurring = isRecurring
                    )
                    contractRepository.updateContract(updatedContract)
                    onSuccess()
                } else {
                    onFailure()
                }
            } catch (e: Exception) {
                onFailure()
            }
        }
    }
}