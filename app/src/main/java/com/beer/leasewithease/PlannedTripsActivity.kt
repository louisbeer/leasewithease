package com.beer.leasewithease

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.PlannedTrip
import com.beer.leasewithease.domain.usecase.PlannedTripSchedule
import com.beer.leasewithease.theme.LeaseWithEaseTheme
import com.beer.leasewithease.theme.customColors
import com.beer.leasewithease.widget.CompactInfoWidget
import com.beer.leasewithease.widget.QuickUpdateWidget
import com.beer.leasewithease.widget.VisualWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manages the longer drives the user already has in the diary for one contract.
 */
@AndroidEntryPoint
class PlannedTripsActivity : ComponentActivity() {

    private val viewModel: PlannedTripsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LeaseWithEaseTheme {
                val context = LocalContext.current
                var showEditor by remember { mutableStateOf(false) }
                var tripBeingEdited by remember { mutableStateOf<PlannedTrip?>(null) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Planned Trips") },
                            navigationIcon = {
                                IconButton(onClick = { (context as? Activity)?.finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            tripBeingEdited = null
                            showEditor = true
                        }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add trip")
                        }
                    }
                ) { padding ->
                    PlannedTripsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding),
                        onTripClick = { trip ->
                            tripBeingEdited = trip
                            showEditor = true
                        }
                    )
                }

                if (showEditor) {
                    val contract by viewModel.contract.collectAsState()
                    TripEditorDialog(
                        trip = tripBeingEdited,
                        contract = contract,
                        onDismiss = { showEditor = false },
                        onSave = { kilometers, date, label ->
                            val existing = tripBeingEdited
                            if (existing == null) {
                                viewModel.addTrip(kilometers, date, label)
                            } else {
                                viewModel.updateTrip(existing, kilometers, date, label)
                            }
                            showEditor = false
                        },
                        onDelete = {
                            tripBeingEdited?.let { viewModel.deleteTrip(it) }
                            showEditor = false
                        }
                    )
                }
            }
        }
    }

    // HomeActivity refreshes the widgets when it comes back to the front, which covers going back
    // from here. Leaving straight for the home screen does not pass through it, so nudge them here
    // too rather than let a widget sit on a balance that no longer holds the trip back.
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            CompactInfoWidget().updateAll(this@PlannedTripsActivity)
            QuickUpdateWidget().updateAll(this@PlannedTripsActivity)
            VisualWidget().updateAll(this@PlannedTripsActivity)
        }
    }

    companion object {
        const val EXTRA_CONTRACT_ID = "contractId"
    }
}

@Composable
fun PlannedTripsScreen(
    viewModel: PlannedTripsViewModel,
    modifier: Modifier = Modifier,
    onTripClick: (PlannedTrip) -> Unit
) {
    val trips by viewModel.trips.collectAsState()
    val contract by viewModel.contract.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Kilometers for trips that are still ahead are held back from your allowance " +
                "right away, so the balance on the home screen already accounts for them. The day " +
                "after a trip they are released again, because by then they show up in your " +
                "odometer reading.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (trips.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No planned trips yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(trips, key = { it.id }) { trip ->
                    PlannedTripRow(
                        trip = trip,
                        contract = contract,
                        onClick = { onTripClick(trip) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedTripRow(trip: PlannedTrip, contract: Contract?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = dateFormat.format(trip.date),
                    style = MaterialTheme.typography.titleMedium
                )
                trip.label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (contract != null) {
                    TripStatusText(trip = trip, contract = contract)
                }
            }
            Text(
                text = "%,d km".format(trip.kilometers),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun TripStatusText(trip: PlannedTrip, contract: Contract) {
    val outsideTerm = !PlannedTripSchedule.isWithinTerm(trip, contract)
    val reserved = PlannedTripSchedule.isReserved(trip, contract)
    val needsReading = PlannedTripSchedule.needsMileageUpdate(trip, contract)

    val (text, color) = when {
        outsideTerm -> "Outside contract term — not counted" to
            MaterialTheme.colorScheme.onSurfaceVariant
        reserved -> "Held back from your allowance" to MaterialTheme.customColors.balanceIndicatorGood
        needsReading -> "Already driven — update your mileage" to
            MaterialTheme.customColors.balanceIndicatorBad
        else -> "Already driven" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
fun TripEditorDialog(
    trip: PlannedTrip?,
    contract: Contract?,
    onDismiss: () -> Unit,
    onSave: (kilometers: Int, date: Date, label: String?) -> Unit,
    onDelete: () -> Unit
) {
    var kilometers by remember { mutableStateOf(trip?.kilometers?.toString() ?: "") }
    var date by remember { mutableStateOf(trip?.date) }
    var label by remember { mutableStateOf(trip?.label ?: "") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val calendar = Calendar.getInstance().apply { date?.let { time = it } }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            date = calendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val enteredKilometers = kilometers.toIntOrNull()
    val chosenDate = date
    val outsideTerm = chosenDate != null && contract != null &&
        !PlannedTripSchedule.isWithinTerm(
            PlannedTrip(contractId = contract.id, kilometers = 0, date = chosenDate),
            contract
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trip == null) "Add Planned Trip" else "Edit Planned Trip") },
        text = {
            Column {
                TextField(
                    value = kilometers,
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() }) {
                            kilometers = newText
                        }
                    },
                    label = { Text("Kilometers*") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    focusManager.clearFocus()
                    datePickerDialog.show()
                }) {
                    Text(text = chosenDate?.let { dateFormat.format(it) } ?: "Select Date*")
                }
                if (outsideTerm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "That date is outside the contract term, so the trip will be kept " +
                            "but not counted against this allowance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = enteredKilometers != null && enteredKilometers > 0 && chosenDate != null,
                onClick = {
                    focusManager.clearFocus()
                    onSave(enteredKilometers!!, chosenDate!!, label)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (trip != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private val dateFormat get() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
