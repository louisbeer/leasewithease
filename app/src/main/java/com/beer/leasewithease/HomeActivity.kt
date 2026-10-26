package com.beer.leasewithease

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.ContractWithTrips
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.domain.usecase.CalculateMileageDiff
import com.beer.leasewithease.domain.usecase.MileageDiffResult
import com.beer.leasewithease.theme.LeaseWithEaseTheme
import com.beer.leasewithease.theme.customColors
import com.beer.leasewithease.ui.home.ContractViewModel
import androidx.glance.appwidget.updateAll
import com.beer.leasewithease.widget.CompactInfoWidget
import com.beer.leasewithease.widget.QuickUpdateWidget
import com.beer.leasewithease.widget.VisualWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    private val contractViewModel: ContractViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openContractId = intent.getLongExtra(EXTRA_OPEN_CONTRACT_ID, -1L)

        setContent {
            LeaseWithEaseTheme {
                val context = LocalContext.current
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = stringResource(id = R.string.app_name)) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            actions = {
                                IconButton(onClick = { context.startActivity(Intent(context, HelpActivity::class.java)) }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_help),
                                        contentDescription = "Help",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                IconButton(onClick = { context.startActivity(Intent(context, AddContractActivity::class.java)) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Contract",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    ContractList(
                        contractViewModel = contractViewModel,
                        modifier = Modifier.padding(padding),
                        openContractId = openContractId
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            CompactInfoWidget().updateAll(this@HomeActivity)
            QuickUpdateWidget().updateAll(this@HomeActivity)
            VisualWidget().updateAll(this@HomeActivity)
        }
    }

    companion object {
        const val EXTRA_OPEN_CONTRACT_ID = "extra_open_contract_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContractList(
    contractViewModel: ContractViewModel,
    modifier: Modifier = Modifier,
    openContractId: Long = -1L
) {
    val contracts by contractViewModel.contracts.collectAsState()
    var selectedContractId by remember { mutableStateOf<Long?>(null) }
    // Resolved from the live list rather than held on to, so the sheet keeps up with trips added
    // on the trips screen and never writes back a contract that has changed underneath it.
    val selectedContractForEdit = contracts.find { it.contract.id == selectedContractId }
    var contractToDelete by remember { mutableStateOf<ContractWithTrips?>(null) }
    var showMileageDecreaseConfirmDialog by remember { mutableStateOf(false) }
    var newMileageValue by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Auto-open the mileage bottom sheet when launched from a widget tap
    var handledWidgetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(contracts) {
        if (openContractId != -1L && !handledWidgetOpen && contracts.isNotEmpty()) {
            val target = contracts.find { it.contract.id == openContractId }
            if (target != null) {
                selectedContractId = target.contract.id
                handledWidgetOpen = true
            }
        }
    }

    if (contracts.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "No contracts yet.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { context.startActivity(Intent(context, AddContractActivity::class.java)) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add one")
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(contracts) { contractWithTrips ->
                ContractCard(
                    contractWithTrips = contractWithTrips,
                    onEditClick = { selectedContractId = contractWithTrips.contract.id },
                    onLongClick = { contractToDelete = contractWithTrips }
                )
            }
        }
    }

    if (selectedContractForEdit != null) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        ModalBottomSheet(
            onDismissRequest = {
                selectedContractId = null
                Toast.makeText(context, "Action was cancelled", Toast.LENGTH_SHORT).show()
            },
            sheetState = sheetState,
            modifier = Modifier.height(screenHeight * 0.5f)
        ) {
            var mileage by remember { mutableStateOf(selectedContractForEdit?.contract?.vehicleMileage.toString()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = mileage,
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() }) {
                            mileage = newText
                        }
                    },
                    label = { Text("New Mileage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                    val newMileage = mileage.toIntOrNull()
                    if (newMileage != null) {
                        if (newMileage < selectedContractForEdit!!.contract.vehicleMileage) {
                            newMileageValue = newMileage
                            showMileageDecreaseConfirmDialog = true
                        } else {
                            scope.launch {
                                try {
                                    contractViewModel.updateMileage(selectedContractForEdit!!.contract, newMileage)
                                    sheetState.hide()
                                    selectedContractId = null
                                    Toast.makeText(context, "Mileage updated successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error updating mileage: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }) {
                    Text("Update mileage")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(context, EditContractActivity::class.java)
                        intent.putExtra("contractId", selectedContractForEdit?.contract?.id?.toInt())
                        context.startActivity(intent)
                    }
                ) {
                    Text("Edit contract")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(context, PlannedTripsActivity::class.java)
                        intent.putExtra(
                            PlannedTripsActivity.EXTRA_CONTRACT_ID,
                            selectedContractForEdit?.contract?.id ?: -1L
                        )
                        context.startActivity(intent)
                    }
                ) {
                    val tripCount = selectedContractForEdit?.plannedTrips?.size ?: 0
                    Text(if (tripCount == 0) "Planned trips" else "Planned trips ($tripCount)")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showMileageDecreaseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMileageDecreaseConfirmDialog = false },
            title = {
                val annotatedString = buildAnnotatedString {
                    append("Are you sure you want to ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("decrease")
                    pop()
                    append(" the mileage of the vehicle?")
                }
                Text(text = annotatedString)
            },
            confirmButton = {
                Button(onClick = {
                    showMileageDecreaseConfirmDialog = false
                    Toast.makeText(context, "Action was cancelled", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Cancel")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                contractViewModel.updateMileage(selectedContractForEdit!!.contract, newMileageValue)
                                showMileageDecreaseConfirmDialog = false
                                sheetState.hide()
                                selectedContractId = null
                                Toast.makeText(context, "Mileage updated successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error updating mileage: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                ) {
                    Text("Decrease")
                }
            }
        )
    }

    if (contractToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                contractToDelete = null
                Toast.makeText(context, "Deletion cancelled", Toast.LENGTH_SHORT).show()
            },
            title = { Text(text = "Delete contract?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                contractViewModel.deleteContract(contractToDelete!!.contract)
                                contractToDelete = null
                                Toast.makeText(context, "Contract deleted successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error deleting contract: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    contractToDelete = null
                    Toast.makeText(context, "Deletion cancelled", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContractCard(
    contractWithTrips: ContractWithTrips,
    onEditClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val contract = contractWithTrips.contract
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = onEditClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            Box {
                val imageRes = when (contract.vehicleType) {
                    VehicleType.CAR -> R.drawable.ic_car_cartoon
                    VehicleType.MOTORCYCLE -> R.drawable.ic_motorcycle_cartoon
                }
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = contract.vehicleName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(ParallelogramShape())
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = contract.contractType.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .width(64.dp)
                        .height(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit Mileage",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = contract.vehicleName,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${contract.vehicleMileage} km",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Kilometer balance indicator
            val diff = CalculateMileageDiff.calculate(contract, contractWithTrips.plannedTrips)
            val kilometerDifference = diff.kilometerDifference
            val costText = diff.costText
            val greenProgress = diff.greenProgress
            val redProgress = diff.redProgress

            val differenceColor = when {
                kilometerDifference > 5 -> MaterialTheme.customColors.balanceIndicatorGood
                kilometerDifference < -5 -> MaterialTheme.customColors.balanceIndicatorBad
                else -> LocalContentColor.current
            }

            Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.typography.headlineSmall.fontSize.value.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth(greenProgress)
                                .fillMaxHeight()
                                .background(MaterialTheme.customColors.balanceIndicatorGood)
                                .align(Alignment.CenterEnd)
                        )
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth(redProgress)
                                .fillMaxHeight()
                                .background(MaterialTheme.customColors.balanceIndicatorBad)
                                .align(Alignment.CenterStart)
                        )
                    }
                }

                Text(
                    text = "%.0f km%s".format(kilometerDifference, costText),
                    color = differenceColor,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                MileagePaceRow(
                    diff = diff,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                PlannedTripNotes(
                    diff = diff,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// Pace the driver may keep from today on to land exactly on the included
// allowance. Floored, so following the figure never overshoots the budget.
@Composable
fun MileagePaceRow(diff: MileageDiffResult, modifier: Modifier = Modifier) {
    val perDay = diff.allowedKmPerDay
    val text = when {
        perDay == null -> "Term ended"
        diff.remainingKilometers < 0 -> "0 km/day · %,.0f km over".format(floor(abs(diff.remainingKilometers)))
        else -> "%,.0f km/day · %,.0f/wk · %,.0f/mo".format(
            floor(perDay),
            floor(perDay * 7),
            floor(perDay * CalculateMileageDiff.DAYS_PER_MONTH)
        )
    }
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}

// Explains the two ways planned trips move the numbers above: kilometers held
// back for trips still ahead, and a trip already driven that the odometer has
// not caught up with yet.
@Composable
fun PlannedTripNotes(diff: MileageDiffResult, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.", Locale.getDefault()) }

    if (diff.reservedKilometers > 0) {
        val nextTripDate = diff.nextReservedTrip?.let { dateFormat.format(it.date) }
        Text(
            text = if (nextTripDate != null) {
                "%,d km held back · next trip %s".format(diff.reservedKilometers, nextTripDate)
            } else {
                "%,d km held back for planned trips".format(diff.reservedKilometers)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
    }

    diff.tripNeedingMileageUpdate?.let { trip ->
        Text(
            text = "Planned trip on %s — update your mileage".format(dateFormat.format(trip.date)),
            color = MaterialTheme.customColors.balanceIndicatorBad,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
    }
}

class ParallelogramShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - 20, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}