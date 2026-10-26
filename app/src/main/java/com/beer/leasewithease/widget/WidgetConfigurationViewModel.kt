package com.beer.leasewithease.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beer.leasewithease.domain.model.ContractWithTrips
import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WidgetConfigurationViewModel @Inject constructor(
    contractRepository: ContractRepository
) : ViewModel() {
    val contracts: StateFlow<List<ContractWithTrips>> = contractRepository.getAllContractsWithTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
