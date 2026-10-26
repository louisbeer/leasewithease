package com.beer.leasewithease.domain.usecase

import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractType
import com.beer.leasewithease.domain.model.PlannedTrip
import com.beer.leasewithease.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import kotlin.math.abs

/**
 * The reservation behaviour of planned trips, as seen through [CalculateMileageDiff].
 */
class PlannedTripReservationTest {

    @Test
    fun `trip still ahead is booked against the allowance in full`() {
        val contract = contract(startOffsetDays = -400, vehicleMileage = 15_000)
        val withoutTrip = CalculateMileageDiff.calculate(contract, now = NOW)
        val withTrip = CalculateMileageDiff.calculate(
            contract = contract,
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = 10)),
            now = NOW
        )

        assertEquals(1_200, withTrip.reservedKilometers)
        // The whole trip hits the balance now, not a pro-rated slice of it.
        assertEquals(withoutTrip.kilometerDifference - 1_200, withTrip.kilometerDifference, 0.01)
        assertEquals(withoutTrip.remainingKilometers - 1_200, withTrip.remainingKilometers, 0.01)
    }

    @Test
    fun `reservation leaves the contract average untouched`() {
        val withTrip = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = 10)),
            now = NOW
        )

        // The plan line stays the honest contract average; only the budget shrinks.
        assertEquals(41.07, withTrip.planKmPerDay, 0.01)
        // 13.800 km of everyday driving left over 329,5 days.
        assertEquals(41.88, withTrip.allowedKmPerDay!!, 0.01)
    }

    @Test
    fun `several trips ahead add up`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(
                trip(kilometers = 1_200, dateOffsetDays = 10),
                trip(kilometers = 800, dateOffsetDays = 40)
            ),
            now = NOW
        )

        assertEquals(2_000, diff.reservedKilometers)
        assertEquals(1_200, diff.nextReservedTrip!!.kilometers)
    }

    @Test
    fun `trip planned for today still holds its kilometers back`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = 0)),
            now = NOW
        )

        assertEquals(1_200, diff.reservedKilometers)
    }

    @Test
    fun `trip that has passed holds nothing back`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = -1)),
            now = NOW
        )

        assertEquals(0, diff.reservedKilometers)
        assertNull(diff.nextReservedTrip)
    }

    @Test
    fun `trip beyond the contract term holds nothing back`() {
        val diff = CalculateMileageDiff.calculate(
            // Term runs 730,5 days from 400 days ago, so it ends in 330 days.
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = 400)),
            now = NOW
        )

        assertEquals(0, diff.reservedKilometers)
    }

    @Test
    fun `trip before the contract starts holds nothing back`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = 10, vehicleMileage = 0),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = 5)),
            now = NOW
        )

        assertEquals(0, diff.reservedKilometers)
    }

    @Test
    fun `balance does not jump when a reservation gives way to the odometer`() {
        val contract = contract(startOffsetDays = -400, vehicleMileage = 15_000)
        val trips = listOf(trip(kilometers = 1_200, dateOffsetDays = 10))

        val dayBefore = CalculateMileageDiff.calculate(
            contract = contract,
            plannedTrips = trips,
            now = NOW + 9 * MILLIS_PER_DAY
        )
        // The day after the trip the reservation is gone and the kilometers are in the odometer.
        val dayAfter = CalculateMileageDiff.calculate(
            contract = contract.copy(vehicleMileage = 16_200),
            plannedTrips = trips,
            now = NOW + 11 * MILLIS_PER_DAY
        )

        assertEquals(1_200, dayBefore.reservedKilometers)
        assertEquals(0, dayAfter.reservedKilometers)
        // Two days of allowance accrued in between, and nothing else moved: no 1.200 km cliff.
        val drift = dayAfter.kilometerDifference - dayBefore.kilometerDifference
        assertTrue(
            "balance moved by " + drift + " km across the trip date",
            abs(drift - 2 * dayBefore.planKmPerDay) < 1.0
        )
    }

    @Test
    fun `reserving more than is left floors the pace at zero`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 28_000),
            plannedTrips = listOf(trip(kilometers = 5_000, dateOffsetDays = 10)),
            now = NOW
        )

        assertEquals(-3_000.0, diff.remainingKilometers, 0.01)
        assertEquals(0.0, diff.allowedKmPerDay!!, 0.01)
    }

    @Test
    fun `trip that has happened is flagged until a reading is entered`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = -10)),
            now = NOW
        )

        assertEquals(1_200, diff.tripNeedingMileageUpdate!!.kilometers)
    }

    @Test
    fun `entering a reading after the trip clears the flag`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 16_200)
                .copy(lastMileageUpdateAt = Date(NOW - 5 * MILLIS_PER_DAY)),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = -10)),
            now = NOW
        )

        assertNull(diff.tripNeedingMileageUpdate)
    }

    @Test
    fun `a reading taken on the day of the trip counts as entered`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 16_200)
                .copy(lastMileageUpdateAt = Date(NOW - 10 * MILLIS_PER_DAY)),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = -10)),
            now = NOW
        )

        assertNull(diff.tripNeedingMileageUpdate)
    }

    @Test
    fun `a reading older than the trip leaves the flag up`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000)
                .copy(lastMileageUpdateAt = Date(NOW - 20 * MILLIS_PER_DAY)),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = -10)),
            now = NOW
        )

        assertEquals(1_200, diff.tripNeedingMileageUpdate!!.kilometers)
    }

    @Test
    fun `a trip still ahead is never flagged for a missing reading`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            plannedTrips = listOf(trip(kilometers = 1_200, dateOffsetDays = 10)),
            now = NOW
        )

        assertNull(diff.tripNeedingMileageUpdate)
    }

    private fun trip(kilometers: Int, dateOffsetDays: Long, label: String? = null) = PlannedTrip(
        contractId = 1L,
        kilometers = kilometers,
        date = Date(NOW + dateOffsetDays * MILLIS_PER_DAY),
        label = label
    )

    private fun contract(
        startOffsetDays: Long,
        vehicleMileage: Int,
        durationInMonths: Int = 24,
        includedKilometers: Int = 30_000
    ) = Contract(
        id = 1L,
        vehicleName = "Test Vehicle",
        vehicleType = VehicleType.CAR,
        vehicleMileage = vehicleMileage,
        mileageAtContractStart = 0,
        contractType = ContractType.LEASING,
        startDate = Date(NOW + startOffsetDays * MILLIS_PER_DAY),
        durationInMonths = durationInMonths,
        includedKilometers = includedKilometers
    )

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
        const val NOW = 1_700_000_000_000L
    }
}
