package com.beer.leasewithease.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.action.clickable
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.beer.leasewithease.HomeActivity
import com.beer.leasewithease.domain.usecase.CalculateMileageDiff
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompactInfoWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 40.dp),   // 2×1: single row
            DpSize(110.dp, 110.dp),  // 2×2: header + value + progress
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

                    if (size.height >= 100.dp) {
                        // 2×2: header chip + prominent value + progress bar
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(GlanceTheme.colors.surface)
                                .cornerRadius(16.dp)
                                .padding(8.dp)
                                .clickable(actionStartActivity<HomeActivity>())
                        ) {
                            VehicleHeaderBand(contract.vehicleName, contract.vehicleType, iconSize = 20.dp)
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MileageValue(diff, valueSize = 26.sp)
                            }
                            MileageDiffProgressBar(diff, barWidth = size.width - 16.dp)
                        }
                    } else {
                        // 2×1: icon + name + value in one line
                        Row(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(GlanceTheme.colors.surface)
                                .cornerRadius(16.dp)
                                .padding(horizontal = 10.dp)
                                .clickable(actionStartActivity<HomeActivity>()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(vehicleIconRes(contract.vehicleType)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(20.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = contract.vehicleName,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight(),
                                maxLines = 1
                            )
                            Text(
                                text = "%.0f km".format(diff.kilometerDifference),
                                style = TextStyle(
                                    color = mileageStatusColor(diff),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    NotConfiguredContent()
                }
            }
        }
    }
}
