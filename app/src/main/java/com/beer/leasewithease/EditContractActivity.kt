package com.beer.leasewithease

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.beer.leasewithease.domain.model.ContractType
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.theme.LeaseWithEaseTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditContractActivity : ComponentActivity() {

    private val viewModel: EditContractViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LeaseWithEaseTheme {
                val context = LocalContext.current
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Edit Contract") },
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
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        EditContractScreen(viewModel = viewModel, onSaveSuccess = {
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            Toast.makeText(this, "Successfully edited contract", Toast.LENGTH_SHORT).show()
                            finish()
                        }, onSaveFailure = {
                            Toast.makeText(this, "Error while saving", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContractScreen(viewModel: EditContractViewModel, onSaveSuccess: () -> Unit, onSaveFailure: () -> Unit) {
    val contract by viewModel.contract.collectAsState()

    var vehicleName by remember { mutableStateOf("") }
    var vehicleMileageAtContractStart by remember { mutableStateOf("0") }
    var selectedVehicleType by remember { mutableStateOf<VehicleType?>(null) }
    var selectedContractType by remember { mutableStateOf<ContractType?>(null) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var duration by remember { mutableStateOf("") }
    var includedKm by remember { mutableStateOf("") }
    var costPerExtraKm by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(contract) {
        contract?.let {
            vehicleName = it.vehicleName
            vehicleMileageAtContractStart = it.mileageAtContractStart.toString()
            selectedVehicleType = it.vehicleType
            selectedContractType = it.contractType
            startDate = it.startDate
            duration = it.durationInMonths.toString()
            includedKm = it.includedKilometers.toString()
            costPerExtraKm = it.costPerExtraKilometer.toString()
            isRecurring = it.isRecurring
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            startDate = calendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextField(
            value = vehicleName,
            onValueChange = { vehicleName = it },
            label = { Text("Vehicle Name*") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = vehicleMileageAtContractStart,
            onValueChange = { newText ->
                if (newText.all { it.isDigit() }) {
                    vehicleMileageAtContractStart = newText
                }
            },
            label = { Text("Mileage at contract start") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle Type Radio Buttons
        Text("Vehicle Type*")
        Row {
            VehicleType.values().forEach { vehicleType ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedVehicleType == vehicleType,
                        onClick = { selectedVehicleType = vehicleType }
                    )
                    Text(text = vehicleType.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Contract Type Radio Buttons
        Text("Contract Type*")
        Row {
            ContractType.values().forEach { contractType ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedContractType == contractType,
                        onClick = { selectedContractType = contractType }
                    )
                    Text(text = contractType.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Start Date Picker
        Button(onClick = { datePickerDialog.show() }) {
            Text(text = startDate?.let { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it) } ?: "Select Start Date*")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = duration,
            onValueChange = { newText ->
                if (newText.all { it.isDigit() }) {
                    duration = newText
                }
            },
            label = { Text("Duration (Months)*") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = includedKm,
            onValueChange = { newText ->
                if (newText.all { it.isDigit() }) {
                    includedKm = newText
                }
            },
            label = { Text("Included Kilometers*") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = costPerExtraKm,
            onValueChange = { newText ->
                if (newText.count { it == '.' } <= 1 && newText.all { it.isDigit() || it == '.' }) {
                    costPerExtraKm = newText
                }
            },
            label = { Text("Cost per Extra Kilometer") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isRecurring,
                onCheckedChange = { isRecurring = it }
            )
            Text(text = "Is Recurring")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (vehicleName.isBlank() || selectedVehicleType == null || selectedContractType == null || startDate == null || duration.isBlank() || includedKm.isBlank()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@Button
            }

            viewModel.updateContract(
                vehicleName = vehicleName,
                vehicleMileage = contract?.vehicleMileage ?: 0,
                mileageAtContractStart = vehicleMileageAtContractStart.toIntOrNull() ?: 0,
                vehicleType = selectedVehicleType!!,
                contractType = selectedContractType!!,
                startDate = startDate!!,
                durationInMonths = duration.toInt(),
                includedKilometers = includedKm.toInt(),
                costPerExtraKilometer = costPerExtraKm.toFloatOrNull() ?: 0f,
                isRecurring = isRecurring,
                onSuccess = { onSaveSuccess() },
                onFailure = { onSaveFailure() }
            )
        }) {
            Text("Save Contract")
        }
    }
}