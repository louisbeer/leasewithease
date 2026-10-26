package com.beer.leasewithease.domain.usecase

import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractType
import com.beer.leasewithease.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CalculateMileageDiffTest {

    @Test
    fun `ahead of plan mid term allows a higher pace than the contract average`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 15_000),
            now = NOW
        )

        // 30.000 km / 730,5 days
        assertEquals(41.07, diff.planKmPerDay, 0.01)
        assertEquals(1_468.17, diff.kilometerDifference, 0.01)
        assertEquals(15_000.0, diff.remainingKilometers, 0.01)
        assertEquals(329.5, diff.remainingDays, 0.01)
        // 15.000 km left over 329,5 days
        assertEquals(45.52, diff.allowedKmPerDay!!, 0.01)
        assertTrue(diff.allowedKmPerDay!! > diff.planKmPerDay)
    }

    @Test
    fun `behind plan mid term allows a lower pace than the contract average`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 20_000),
            now = NOW
        )

        assertEquals(-3_531.83, diff.kilometerDifference, 0.01)
        assertEquals(10_000.0, diff.remainingKilometers, 0.01)
        // 10.000 km left over 329,5 days
        assertEquals(30.35, diff.allowedKmPerDay!!, 0.01)
        assertTrue(diff.allowedKmPerDay!! < diff.planKmPerDay)
    }

    @Test
    fun `contract that has not started yet allows exactly the contract average`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = 10, vehicleMileage = 0),
            now = NOW
        )

        assertEquals(0.0, diff.kilometerDifference, 0.01)
        assertEquals(730.5, diff.remainingDays, 0.01)
        assertEquals(diff.planKmPerDay, diff.allowedKmPerDay!!, 0.01)
    }

    @Test
    fun `expired term reports no pace`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -800, vehicleMileage = 28_000),
            now = NOW
        )

        assertTrue(diff.remainingDays < 0)
        assertNull(diff.allowedKmPerDay)
    }

    @Test
    fun `allowance already spent floors the pace at zero`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(startOffsetDays = -400, vehicleMileage = 32_000),
            now = NOW
        )

        assertEquals(-2_000.0, diff.remainingKilometers, 0.01)
        assertEquals(0.0, diff.allowedKmPerDay!!, 0.01)
    }

    @Test
    fun `contract without duration or allowance reports no pace`() {
        val diff = CalculateMileageDiff.calculate(
            contract = contract(
                startOffsetDays = -400,
                vehicleMileage = 0,
                durationInMonths = 0,
                includedKilometers = 0
            ),
            now = NOW
        )

        assertEquals(0.0, diff.planKmPerDay, 0.01)
        assertEquals(0f, diff.progress, 0.001f)
        assertNull(diff.allowedKmPerDay)
    }

    private fun contract(
        startOffsetDays: Long,
        vehicleMileage: Int,
        mileageAtContractStart: Int = 0,
        durationInMonths: Int = 24,
        includedKilometers: Int = 30_000
    ) = Contract(
        vehicleName = "Test Vehicle",
        vehicleType = VehicleType.CAR,
        vehicleMileage = vehicleMileage,
        mileageAtContractStart = mileageAtContractStart,
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
