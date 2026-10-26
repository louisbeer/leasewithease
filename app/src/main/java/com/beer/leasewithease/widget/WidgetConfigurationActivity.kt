package com.beer.leasewithease.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.beer.leasewithease.domain.model.Contract
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.domain.usecase.CalculateMileageDiff
import com.beer.leasewithease.theme.LeaseWithEaseTheme
import com.beer.leasewithease.theme.customColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WidgetConfigurationActivity : ComponentActivity() {

    private val viewModel: WidgetConfigurationViewModel by viewModels()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Set cancel result immediately — if the user backs out, the widget is not placed
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        setContent {
            LeaseWithEaseTheme {
                val contracts by viewModel.contracts.collectAsState()
                VehiclePickerScreen(
                    contracts = contracts,
                    onContractSelected = { saveSelectionAndFinish(it) }
                )
            }
        }
    }

    private fun saveSelectionAndFinish(contract: Contract) {
        lifecycleScope.launch {
            try {
                // getGlanceIdBy(Intent) reads the appWidgetId straight from the extras and never
                // touches AppWidgetManager.getAppWidgetInfo(), which returns null (and would make
                // getGlanceIdBy(Int) throw) while the widget is still being placed for the first time.
                val glanceManager = GlanceAppWidgetManager(this@WidgetConfigurationActivity)
                val glanceId = glanceManager.getGlanceIdBy(intent) ?: glanceManager.getGlanceIdBy(appWidgetId)

                // Persist the selected contractId in this widget instance's Glance state
                updateAppWidgetState(
                    context = this@WidgetConfigurationActivity,
                    glanceId = glanceId
                ) { prefs ->
                    prefs[CONTRACT_ID_KEY] = contract.id
                }

                // Push the new selection to the widget. This is what makes the choice take effect
                // right away: if a Glance session is already running for this instance (it is on
                // first placement, because the placeholder was just composed) the update hands it
                // the state we wrote above; otherwise it starts a session and runs provideGlance.
                widgetFor(appWidgetId)?.update(this@WidgetConfigurationActivity, glanceId)
            } catch (e: Exception) {
                // Widget will update on its next scheduled cycle if the above fails
            }

            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            finish()
        }
    }

    // Which of the three widget types this instance is. getAppWidgetIds() is used rather than
    // getAppWidgetInfo() because it is scoped to our own provider and stays reliable while the
    // widget is still being placed.
    private fun widgetFor(appWidgetId: Int): GlanceAppWidget? {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        fun belongsTo(receiver: Class<*>) = appWidgetManager
            .getAppWidgetIds(ComponentName(this, receiver))
            .contains(appWidgetId)

        return when {
            belongsTo(CompactInfoWidgetReceiver::class.java) -> CompactInfoWidget()
            belongsTo(QuickUpdateWidgetReceiver::class.java) -> QuickUpdateWidget()
            belongsTo(VisualWidgetReceiver::class.java) -> VisualWidget()
            else -> null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiclePickerScreen(
    contracts: List<Contract>,
    onContractSelected: (Contract) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Vehicle") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (contracts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No vehicles found.\nAdd a contract in the app first.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(contracts) { contract ->
                    VehiclePickerItem(
                        contract = contract,
                        onClick = { onContractSelected(contract) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun VehiclePickerItem(contract: Contract, onClick: () -> Unit) {
    val diff = CalculateMileageDiff.calculate(contract)
    val diffColor = when {
        diff.kilometerDifference > 5 -> MaterialTheme.customColors.balanceIndicatorGood
        diff.kilometerDifference < -5 -> MaterialTheme.customColors.balanceIndicatorBad
        else -> MaterialTheme.colorScheme.onSurface
    }
    val typeLabel = when (contract.vehicleType) {
        VehicleType.CAR -> "CAR"
        VehicleType.MOTORCYCLE -> "MOTO"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vehicle type badge
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.weight(0.05f))
        // Vehicle name and contract info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contract.vehicleName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${contract.contractType.name} · ${contract.vehicleMileage} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(0.02f))
        // Current diff value
        Text(
            text = "%.0f km".format(diff.kilometerDifference),
            style = MaterialTheme.typography.titleSmall,
            color = diffColor,
            fontWeight = FontWeight.Bold
        )
    }
}
