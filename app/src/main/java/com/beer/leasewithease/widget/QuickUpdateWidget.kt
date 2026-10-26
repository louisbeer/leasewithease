package com.beer.leasewithease.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import com.beer.leasewithease.HomeActivity
import com.beer.leasewithease.R
import com.beer.leasewithease.domain.usecase.CalculateMileageDiff
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuickUpdateWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 40.dp),   // 3×1: name + value + button in a row
            DpSize(180.dp, 110.dp),  // 3×2: header + value + full-width button
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val contractId = prefs[CONTRACT_ID_KEY]

        val contract = contractId?.let {
            withContext(Dispatchers.IO) {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                ).contractRepository().getContractById(it.toInt())
            }
        }

        provideContent {
            GlanceTheme(colors = LeaseWithEaseWidgetColors) {
                if (contract != null) {
                    val diff = CalculateMileageDiff.calculate(contract)
                    val size = LocalSize.current

                    // Opening HomeActivity with the contract ID triggers the mileage update bottom sheet
                    val contractIdParam = ActionParameters.Key<Long>(HomeActivity.EXTRA_OPEN_CONTRACT_ID)
                    val updateAction = actionStartActivity<HomeActivity>(
                        actionParametersOf(contractIdParam to contract.id)
                    )
                    val openHomeAction = actionStartActivity<HomeActivity>()

                    if (size.height >= 100.dp) {
                        // 3×2: header chip, centered value, full-width update button
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(GlanceTheme.colors.surface)
                                .cornerRadius(16.dp)
                                .padding(8.dp)
                                .clickable(openHomeAction)
                        ) {
                            VehicleHeaderBand(contract.vehicleName, contract.vehicleType, iconSize = 20.dp)
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MileageValue(diff, valueSize = 22.sp)
                            }
                            UpdateButtonWide(updateAction)
                        }
                    } else {
                        // 3×1: compact row with a square update button
                        Row(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(GlanceTheme.colors.surface)
                                .cornerRadius(16.dp)
                                .padding(horizontal = 10.dp)
                                .clickable(openHomeAction),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = contract.vehicleName,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "%.0f km".format(diff.kilometerDifference),
                                    style = TextStyle(
                                        color = mileageStatusColor(diff),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            UpdateButtonSquare(updateAction)
                        }
                    }
                } else {
                    NotConfiguredContent()
                }
            }
        }
    }
}

@Composable
private fun UpdateButtonWide(action: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.primary)
            .cornerRadius(10.dp)
            .padding(vertical = 7.dp)
            .clickable(action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_edit),
            contentDescription = "Update mileage",
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = "Update mileage",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun UpdateButtonSquare(action: Action) {
    Box(
        modifier = GlanceModifier
            .size(36.dp)
            .background(GlanceTheme.colors.primary)
            .cornerRadius(8.dp)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_edit),
            contentDescription = "Update mileage",
            modifier = GlanceModifier.size(18.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
}
