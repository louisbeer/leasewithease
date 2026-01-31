package com.beer.leasewithease.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContractViewModel @Inject constructor(private val contractRepository: ContractRepository) : ViewModel() {

    val contracts: StateFlow<List<Contract>> = contractRepository.getAllContracts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateContract(contract: Contract) {
        viewModelScope.launch {
            contractRepository.updateContract(contract)
        }
    }

    fun deleteContract(contract: Contract) {
        viewModelScope.launch {
            contractRepository.deleteContract(contract)
        }
    }
}
