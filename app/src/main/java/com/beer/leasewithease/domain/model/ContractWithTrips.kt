package com.beer.leasewithease.domain.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A contract together with every trip planned against it, so the home list can render balances in a
 * single query instead of one lookup per card.
 */
data class ContractWithTrips(
    @Embedded val contract: Contract,
    @Relation(parentColumn = "id", entityColumn = "contractId")
    val plannedTrips: List<PlannedTrip> = emptyList()
)
