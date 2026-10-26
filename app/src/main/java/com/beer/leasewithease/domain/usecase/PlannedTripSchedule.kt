package com.beer.leasewithease.domain.usecase

import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.PlannedTrip
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Decides which planned trips currently hold kilometers back from a contract's allowance.
 *
 * Comparisons run on whole local days rather than raw timestamps, so a trip planned for today still
 * counts for the whole of today no matter what time of day the balance is read, and daylight saving
 * cannot shift a trip into the neighbouring day.
 */
object PlannedTripSchedule {

    /**
     * Kilometers held back from the allowance right now: every trip still ahead of us that falls
     * inside the contract term.
     */
    fun reservedKilometers(
        contract: Contract,
        plannedTrips: List<PlannedTrip>,
        now: Long = System.currentTimeMillis()
    ): Int = plannedTrips.filter { isReserved(it, contract, now) }.sumOf { it.kilometers }

    /** The soonest trip still holding kilometers back, for labelling the reservation in the UI. */
    fun nextReservedTrip(
        contract: Contract,
        plannedTrips: List<PlannedTrip>,
        now: Long = System.currentTimeMillis()
    ): PlannedTrip? = plannedTrips
        .filter { isReserved(it, contract, now) }
        .minByOrNull { it.date.time }

    /**
     * A trip holds its kilometers back while its day has not passed yet. From the day after, the
     * reservation is dropped: the kilometers are expected in the odometer reading instead.
     */
    fun isReserved(
        trip: PlannedTrip,
        contract: Contract,
        now: Long = System.currentTimeMillis()
    ): Boolean = isWithinTerm(trip, contract) && startOfDay(trip.date.time) >= startOfDay(now)

    /**
     * Whether the trip falls inside the contract term at all. Trips outside it are kept — the user
     * may be planning around a renewal — but they never touch this contract's budget.
     */
    fun isWithinTerm(trip: PlannedTrip, contract: Contract): Boolean {
        val tripDay = startOfDay(trip.date.time)
        return tripDay >= startOfDay(contract.startDate.time) && tripDay <= contractEnd(contract)
    }

    /**
     * The most recent trip that has already happened but is not reflected in the odometer yet, i.e.
     * no reading has been entered since the trip. Its kilometers are currently counted nowhere, so
     * the balance reads better than it really is until the user updates the mileage.
     */
    fun tripNeedingMileageUpdate(
        contract: Contract,
        plannedTrips: List<PlannedTrip>,
        now: Long = System.currentTimeMillis()
    ): PlannedTrip? = plannedTrips
        .filter { needsMileageUpdate(it, contract, now) }
        .maxByOrNull { it.date.time }

    fun needsMileageUpdate(
        trip: PlannedTrip,
        contract: Contract,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isWithinTerm(trip, contract)) return false
        val tripDay = startOfDay(trip.date.time)
        if (tripDay >= startOfDay(now)) return false
        val lastUpdate = contract.lastMileageUpdateAt ?: return true
        // A reading taken on the day of the trip already includes it.
        return startOfDay(lastUpdate.time) < tripDay
    }

    private fun contractEnd(contract: Contract): Long {
        val durationInDays = contract.durationInMonths * CalculateMileageDiff.DAYS_PER_MONTH
        return contract.startDate.time + (durationInDays * MILLIS_PER_DAY).toLong()
    }

    /** Midnight at the start of the local day containing [timeMillis]. */
    fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Midnight at the start of the local day containing [date]. */
    fun startOfDay(date: Date): Long = startOfDay(date.time)

    private val MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1)
}
