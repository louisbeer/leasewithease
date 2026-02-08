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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.theme.LeaseWithEaseTheme
import com.beer.leasewithease.theme.customColors
import com.beer.leasewithease.ui.home.ContractViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    private val contractViewModel: ContractViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    ContractList(contractViewModel = contractViewModel, modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContractList(contractViewModel: ContractViewModel, modifier: Modifier = Modifier) {
    val contracts by contractViewModel.contracts.collectAsState()
    var selectedContractForEdit by remember { mutableStateOf<Contract?>(null) }
    var contractToDelete by remember { mutableStateOf<Contract?>(null) }
    var showMileageDecreaseConfirmDialog by remember { mutableStateOf(false) }
    var newMileageValue by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            items(contracts) { contract ->
                ContractCard(
                    contract = contract,
                    onEditClick = { selectedContractForEdit = contract },
                    onLongClick = { contractToDelete = contract }
                )
            }
        }
    }

    if (selectedContractForEdit != null) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        ModalBottomSheet(
            onDismissRequest = {
                selectedContractForEdit = null
                Toast.makeText(context, "Action was cancelled", Toast.LENGTH_SHORT).show()
            },
            sheetState = sheetState,
            modifier = Modifier.height(screenHeight * 0.3f)
        ) {
            var mileage by remember { mutableStateOf(selectedContractForEdit?.vehicleMileage.toString()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
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
                Button(onClick = {
                    val newMileage = mileage.toIntOrNull()
                    if (newMileage != null) {
                        if (newMileage < selectedContractForEdit!!.vehicleMileage) {
                            newMileageValue = newMileage
                            showMileageDecreaseConfirmDialog = true
                        } else {
                            scope.launch {
                                try {
                                    val updatedContract = selectedContractForEdit!!.copy(vehicleMileage = newMileage)
                                    contractViewModel.updateContract(updatedContract)
                                    sheetState.hide()
                                    selectedContractForEdit = null
                                    Toast.makeText(context, "Mileage updated successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error updating mileage: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }) {
                    Text("Save")
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
                                val updatedContract = selectedContractForEdit!!.copy(vehicleMileage = newMileageValue)
                                contractViewModel.updateContract(updatedContract)
                                showMileageDecreaseConfirmDialog = false
                                sheetState.hide()
                                selectedContractForEdit = null
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
                                contractViewModel.deleteContract(contractToDelete!!)
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
fun ContractCard(contract: Contract, onEditClick: () -> Unit, onLongClick: () -> Unit) {
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
            val contractDurationInDays = contract.durationInMonths * 30.4375 // Average days in a month
            val daysSinceContractStart = if (contract.startDate.time > 0 && contract.startDate.time <= System.currentTimeMillis()) {
                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - contract.startDate.time) + 1
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

            val differenceColor = when {
                kilometerDifference > 5 -> MaterialTheme.customColors.balanceIndicatorGood
                kilometerDifference < -5 -> MaterialTheme.customColors.balanceIndicatorBad
                else -> LocalContentColor.current
            }

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

            val greenProgress = (progress).coerceIn(0f, 1f)
            val redProgress = (-progress).coerceIn(0f, 1f)

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
            }
        }
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