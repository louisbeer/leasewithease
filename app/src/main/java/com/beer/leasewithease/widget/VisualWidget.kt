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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import androidx.glance.action.clickable
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.beer.leasewithease.HomeActivity
import com.beer.leasewithease.R
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.domain.usecase.CalculateMileageDiff

class VisualWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 110.dp),  // 4×2: image beside info
            DpSize(250.dp, 180.dp),  // 4×3: full layout with image on top + progress bar
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialContract = loadConfiguredContract(context, id)

        provideContent {
            GlanceTheme(colors = LeaseWithEaseWidgetColors) {
                val contractWithTrips = rememberConfiguredContract(initialContract)
                if (contractWithTrips != null) {
                    val contract = contractWithTrips.contract
                    val diff = CalculateMileageDiff.calculate(
                        contract,
                        contractWithTrips.plannedTrips
                    )
                    val size = LocalSize.current
                    val diffColor = when {
                        diff.kilometerDifference > 5 -> WidgetGreenColor
                        diff.kilometerDifference < -5 -> WidgetRedColor
                        else -> GlanceTheme.colors.onSurface
                    }
                    val diffText = "%.0f km%s".format(diff.kilometerDifference, diff.costText)
                    val imageRes = when (contract.vehicleType) {
                        VehicleType.CAR -> R.drawable.ic_car_cartoon
                        VehicleType.MOTORCYCLE -> R.drawable.ic_motorcycle_cartoon
                    }

                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.surface)
                            .cornerRadius(16.dp)
                            .clickable(actionStartActivity<HomeActivity>())
                    ) {
                        if (size.height >= 170.dp) {
                            // 4×3: full layout — image on top, info below
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                // Vehicle image with contract type label overlay
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .defaultWeight()
                                ) {
                                    Image(
                                        provider = ImageProvider(imageRes),
                                        contentDescription = contract.vehicleName,
                                        modifier = GlanceModifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Contract type badge (top-left overlay)
                                    Box(
                                        modifier = GlanceModifier
                                            .wrapContentSize()
                                            .background(GlanceTheme.colors.primary)
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = contract.contractType.name,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                // Info section below image
                                Column(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = contract.vehicleName,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onSurface,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = GlanceModifier.defaultWeight(),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = diffText,
                                            style = TextStyle(
                                                color = diffColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                    MileageDiffProgressBar(diff, size.width - 20.dp)
                                }
                            }
                        } else {
                            // 4×2: image on the left, info on the right
                            Row(modifier = GlanceModifier.fillMaxSize()) {
                                Box(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .fillMaxHeight()
                                ) {
                                    Image(
                                        provider = ImageProvider(imageRes),
                                        contentDescription = contract.vehicleName,
                                        modifier = GlanceModifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Contract type badge overlay
                                    Box(
                                        modifier = GlanceModifier
                                            .wrapContentSize()
                                            .background(GlanceTheme.colors.primary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = contract.contractType.name,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onPrimary,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                                // Right side info
                                Column(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .fillMaxHeight()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = contract.vehicleName,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = GlanceModifier.height(4.dp))
                                    Text(
                                        text = diffText,
                                        style = TextStyle(
                                            color = diffColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    NotConfiguredContent()
                }
            }
        }
    }
}
