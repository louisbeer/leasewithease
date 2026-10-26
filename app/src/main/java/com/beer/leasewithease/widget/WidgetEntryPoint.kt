package com.beer.leasewithease.widget

import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun contractRepository(): ContractRepository
}
