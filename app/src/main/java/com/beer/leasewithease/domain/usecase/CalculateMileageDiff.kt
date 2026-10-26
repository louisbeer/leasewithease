package com.beer.leasewithease.domain.usecase

import com.beer.leasewithease.domain.model.Contract
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class MileageDiffResult(
    val kilometerDifference: Double,
    val costText: String,
    val progress: Float,
    val greenProgress: Float,
    val redProgress: Float
)

object CalculateMileageDiff {
    fun calculate(contract: Contract): MileageDiffResult {
        val contractDurationInDays = contract.durationInMonths * 30.4375
        val now = System.currentTimeMillis()
        val daysSinceContractStart = if (contract.startDate.time > 0 && contract.startDate.time <= now) {
            TimeUnit.MILLISECONDS.toDays(now - contract.startDate.time) + 1
        } else {
            0L
        }
        val allowedKilometersForToday = if (contractDurationInDays > 0) {
            (contract.includedKilometers.toDouble() / contractDurationInDays) * daysSinceContractStart
        } else {
            0.0
        }
        val actualUsedKilometers = (contract.vehicleMileage - contract.mileageAtContractStart).coerceAtLeast(0)
        val kilometerDifference = allowedKilometersForToday - actualUsedKilometers

        val costText = if (kilometerDifference < 0 && contract.costPerExtraKilometer > 0) {
            val extraCost = abs(kilometerDifference) * contract.costPerExtraKilometer
            " (%.2f€)".format(extraCost)
        } else {
            ""
        }

        val tenPercentOfTotalKm = contract.includedKilometers * 0.1
        val progress = if (tenPercentOfTotalKm > 0) {
            (kilometerDifference / tenPercentOfTotalKm).toFloat().coerceIn(-1f, 1f)
        } else {
            0f
        }

        return MileageDiffResult(
            kilometerDifference = kilometerDifference,
            costText = costText,
            progress = progress,
            greenProgress = progress.coerceIn(0f, 1f),
            redProgress = (-progress).coerceIn(0f, 1f)
        )
    }
}
