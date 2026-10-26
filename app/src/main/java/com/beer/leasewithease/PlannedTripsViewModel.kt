package com.beer.leasewithease

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.PlannedTrip
import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PlannedTripsViewModel @Inject constructor(
    private val contractRepository: ContractRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contractId: Long =
        savedStateHandle.get<Long>(PlannedTripsActivity.EXTRA_CONTRACT_ID) ?: -1L

    private val _contract = MutableStateFlow<Contract?>(null)
    val contract: StateFlow<Contract?> = _contract.asStateFlow()

    val trips: StateFlow<List<PlannedTrip>> = contractRepository
        .getTripsForContract(contractId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _contract.value = contractRepository.getContractWithTripsById(contractId)?.contract
        }
    }

    fun addTrip(kilometers: Int, date: Date, label: String?) {
        viewModelScope.launch {
            contractRepository.insertTrip(
                PlannedTrip(
                    contractId = contractId,
                    kilometers = kilometers,
                    date = date,
                    label = label?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    fun updateTrip(trip: PlannedTrip, kilometers: Int, date: Date, label: String?) {
        viewModelScope.launch {
            contractRepository.updateTrip(
                trip.copy(
                    kilometers = kilometers,
                    date = date,
                    label = label?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    fun deleteTrip(trip: PlannedTrip) {
        viewModelScope.launch {
            contractRepository.deleteTrip(trip)
        }
    }
}
