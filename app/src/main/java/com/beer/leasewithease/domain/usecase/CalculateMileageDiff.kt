package com.beer.leasewithease.domain.usecase

import com.beer.leasewithease.domain.model.Contract
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class MileageDiffResult(
    val kilometerDifference: Double,
    val costText: String,
    val progress: Float,
    val greenProgress: Float,
    val redProgress: Float,
    val planKmPerDay: Double,
    val remainingKilometers: Double,
    val remainingDays: Double,
    val allowedKmPerDay: Double?
)

object CalculateMileageDiff {

    // Average days in a month. Used for the contract duration and for the
    // per-month pace figure alike, so the two always agree.
    const val DAYS_PER_MONTH = 30.4375

    fun calculate(contract: Contract, now: Long = System.currentTimeMillis()): MileageDiffResult {
        val contractDurationInDays = contract.durationInMonths * DAYS_PER_MONTH
        val daysSinceContractStart = if (contract.startDate.time > 0 && contract.startDate.time <= now) {
            TimeUnit.MILLISECONDS.toDays(now - contract.startDate.time) + 1
        } else {
            0L
        }
        val planKmPerDay = if (contractDurationInDays > 0) {
            contract.includedKilometers.toDouble() / contractDurationInDays
        } else {
            0.0
        }
        val allowedKilometersForToday = planKmPerDay * daysSinceContractStart
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

        // Pace the driver may keep from today on to finish exactly on the
        // included allowance. Shares daysSinceContractStart with the balance
        // above so both numbers always describe the same day.
        val remainingKilometers = contract.includedKilometers - actualUsedKilometers.toDouble()
        val remainingDays = contractDurationInDays - daysSinceContractStart
        val allowedKmPerDay = if (remainingDays > 0) {
            (remainingKilometers / remainingDays).coerceAtLeast(0.0)
        } else {
            null
        }

        return MileageDiffResult(
            kilometerDifference = kilometerDifference,
            costText = costText,
            progress = progress,
            greenProgress = progress.coerceIn(0f, 1f),
            redProgress = (-progress).coerceIn(0f, 1f),
            planKmPerDay = planKmPerDay,
            remainingKilometers = remainingKilometers,
            remainingDays = remainingDays,
            allowedKmPerDay = allowedKmPerDay
        )
    }
}
