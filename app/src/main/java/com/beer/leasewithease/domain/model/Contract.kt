package com.beer.leasewithease.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class Contract(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleName: String,
    val vehicleType: VehicleType,
    val vehicleMileage: Int = 0,
    val mileageAtContractStart: Int = 0,
    val contractType: ContractType,
    val startDate: Date,
    val durationInMonths: Int,
    val includedKilometers: Int,
    val costPerExtraKilometer: Float = 0f,
    val isRecurring: Boolean = false
)

enum class ContractType {
    LEASING, INSURANCE
}

enum class VehicleType {
    CAR, MOTORCYCLE
}
