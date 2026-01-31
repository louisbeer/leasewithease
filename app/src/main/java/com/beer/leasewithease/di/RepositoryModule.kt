package com.beer.leasewithease.di

import com.beer.leasewithease.data.local.ContractDao
import com.beer.leasewithease.domain.repository.ContractRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideContractRepository(contractDao: ContractDao): ContractRepository {
        return ContractRepository(contractDao)
    }
}
