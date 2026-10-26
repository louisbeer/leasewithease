package com.beer.leasewithease.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.beer.leasewithease.domain.model.Contract
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the contract selected for this widget instance, for use as the initial value passed to
 * [rememberConfiguredContract]. Call this from `provideGlance` before `provideContent` so the first
 * frame already shows the vehicle instead of a placeholder.
 */
internal suspend fun loadConfiguredContract(context: Context, id: GlanceId): Contract? {
    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
    return prefs[CONTRACT_ID_KEY]?.let { loadContract(context, it) }
}

/**
 * Tracks the contract selected for this widget instance from inside the composition.
 *
 * This has to be observed in the composition rather than read once in `provideGlance`: when the
 * configuration activity stores a new selection, Glance hands the updated state to the *running*
 * composition and does not re-run `provideGlance`. A contract captured in `provideGlance` would
 * therefore stay null for as long as the session lives, leaving a freshly configured widget stuck
 * on the "tap to select vehicle" placeholder.
 *
 * @param initial the contract already loaded in `provideGlance`, so the first frame is not blank.
 */
@Composable
internal fun rememberConfiguredContract(initial: Contract?): Contract? {
    val context = LocalContext.current
    val contractId = currentState(CONTRACT_ID_KEY)
    val contract by produceState(initial, contractId) {
        value = contractId?.let { loadContract(context, it) }
    }
    return contract
}

private suspend fun loadContract(context: Context, contractId: Long): Contract? =
    withContext(Dispatchers.IO) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).contractRepository().getContractById(contractId.toInt())
    }
