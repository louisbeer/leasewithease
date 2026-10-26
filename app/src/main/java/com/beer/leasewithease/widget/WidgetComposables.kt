package com.beer.leasewithease.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.beer.leasewithease.R
import com.beer.leasewithease.domain.model.VehicleType
import com.beer.leasewithease.domain.usecase.MileageDiffResult

internal fun vehicleIconRes(vehicleType: VehicleType): Int = when (vehicleType) {
    VehicleType.CAR -> R.drawable.ic_car_cartoon
    VehicleType.MOTORCYCLE -> R.drawable.ic_motorcycle_cartoon
}

// Short status caption for the mileage difference. Positive = kilometres to spare.
internal fun mileageCaption(diff: MileageDiffResult): String = when {
    diff.kilometerDifference > 5 -> "under budget"
    diff.kilometerDifference < -5 -> "over budget"
    else -> "on budget"
}

// Colour for the mileage value: green when ahead, red when over, neutral otherwise.
@Composable
internal fun mileageStatusColor(diff: MileageDiffResult): ColorProvider = when {
    diff.kilometerDifference > 5 -> WidgetGreenColor
    diff.kilometerDifference < -5 -> WidgetRedColor
    else -> GlanceTheme.colors.onSurface
}

@Composable
internal fun NotConfiguredContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(configureWidgetAction()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tap to\nselect vehicle",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp
            )
        )
    }
}

// Opens the vehicle picker for this widget instance. Each instance needs its own intent data,
// otherwise the PendingIntents are conflated (Intent.filterEquals ignores extras) and every widget
// would reconfigure whichever one was tapped first.
@Composable
private fun configureWidgetAction(): Action {
    val context = LocalContext.current
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(LocalGlanceId.current)
    val intent = Intent(context, WidgetConfigurationActivity::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .setData(Uri.parse("leasewithease://widget/$appWidgetId"))
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    return actionStartActivity(intent)
}

// Inset, rounded header chip: vehicle icon + name on the primary colour.
@Composable
internal fun VehicleHeaderBand(name: String, vehicleType: VehicleType, iconSize: Dp = 22.dp) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.primary)
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(vehicleIconRes(vehicleType)),
            contentDescription = null,
            modifier = GlanceModifier.size(iconSize)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = name,
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

// Big mileage number with a "km" unit and a small status caption underneath.
@Composable
internal fun MileageValue(
    diff: MileageDiffResult,
    valueSize: TextUnit,
    showCaption: Boolean = true
) {
    val statusColor = mileageStatusColor(diff)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "%.0f".format(diff.kilometerDifference),
            style = TextStyle(color = statusColor, fontSize = valueSize, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(
            text = "km",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            maxLines = 1
        )
    }
    if (showCaption) {
        Text(
            text = mileageCaption(diff) + diff.costText,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            maxLines = 1
        )
    }
}

// Dual-sided progress bar: green fills left-side from center, red fills right-side from center.
// Replicates the visual in the app's ContractCard. Glance has no fractional weight modifier,
// so segment widths are computed in dp from the caller-supplied bar width.
@Composable
internal fun MileageDiffProgressBar(diff: MileageDiffResult, barWidth: Dp) {
    val halfWidth = (barWidth - 1.dp) / 2  // account for the 1.dp center divider
    val greenWidth = halfWidth * diff.greenProgress.coerceIn(0f, 1f)
    val redWidth = halfWidth * diff.redProgress.coerceIn(0f, 1f)

    Row(
        modifier = GlanceModifier
            .width(barWidth)
            .height(8.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(4.dp)
    ) {
        // Left half — green indicator aligned to the right (fills from center toward left)
        Box(
            modifier = GlanceModifier.width(halfWidth).fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (greenWidth > 0.dp) {
                Box(modifier = GlanceModifier.width(greenWidth).fillMaxHeight().background(WidgetGreenColor)) {}
            }
        }
        // Center divider
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(GlanceTheme.colors.onSurface)
        ) {}
        // Right half — red indicator aligned to the left (fills from center toward right)
        Box(
            modifier = GlanceModifier.width(halfWidth).fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (redWidth > 0.dp) {
                Box(modifier = GlanceModifier.width(redWidth).fillMaxHeight().background(WidgetRedColor)) {}
            }
        }
    }
}
