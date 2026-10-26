package com.beer.leasewithease.domain.usecase

import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.PlannedTrip
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
    val allowedKmPerDay: Double?,
    /** Kilometers currently held back for trips still ahead. See [PlannedTripSchedule]. */
    val reservedKilometers: Int = 0,
    /** The soonest trip holding kilometers back, for labelling the reservation. */
    val nextReservedTrip: PlannedTrip? = null,
    /** A trip that has happened but is not in the odometer reading yet, if any. */
    val tripNeedingMileageUpdate: PlannedTrip? = null
)

object CalculateMileageDiff {

    // Average days in a month. Used for the contract duration and for the
    // per-month pace figure alike, so the two always agree.
    const val DAYS_PER_MONTH = 30.4375

    fun calculate(
        contract: Contract,
        plannedTrips: List<PlannedTrip> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): MileageDiffResult {
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

        // Trips still ahead are booked against the allowance in full, right away: the balance is
        // meant to answer "do I get by *including* that trip?". Charging the whole trip up front
        // also keeps the balance steady across the trip date, where the reservation gives way to
        // the kilometers appearing in the odometer reading.
        val reservedKilometers = PlannedTripSchedule.reservedKilometers(contract, plannedTrips, now)
        val spokenForKilometers = actualUsedKilometers + reservedKilometers
        val kilometerDifference = allowedKilometersForToday - spokenForKilometers

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
        // above so both numbers always describe the same day. The reservation is
        // carved out first, so the pace covers everyday driving only.
        val remainingKilometers = contract.includedKilometers - spokenForKilometers.toDouble()
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
            allowedKmPerDay = allowedKmPerDay,
            reservedKilometers = reservedKilometers,
            nextReservedTrip = PlannedTripSchedule.nextReservedTrip(contract, plannedTrips, now),
            tripNeedingMileageUpdate =
                PlannedTripSchedule.tripNeedingMileageUpdate(contract, plannedTrips, now)
        )
    }
}
