package com.twinhearts.app.feature.meetplanner.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.feature.meetplanner.domain.isSelected
import com.twinhearts.app.feature.meetplanner.ui.components.CompleteDialog
import com.twinhearts.app.feature.meetplanner.ui.components.DeleteDialog
import com.twinhearts.app.feature.meetplanner.ui.components.EditDialog
import com.twinhearts.app.feature.meetplanner.ui.components.OptionsSheet
import com.twinhearts.app.feature.meetplanner.ui.components.PhotoViewer
import com.twinhearts.app.feature.meetplanner.ui.components.PlaceSheet
import com.twinhearts.app.feature.meetplanner.ui.screens.DetailScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.ItineraryScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.MeetHomeScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.PlanScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.RouteScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.SavedScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.SearchScreen
import com.twinhearts.app.feature.meetplanner.ui.screens.StopsScreen
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPlannerStyled

/**
 * Why this file exists: the one entry point of Meet Planner. It owns the ViewModel, turns one-off
 * effects (toast, share, copy, open map link, location permission) into Android calls, and shows the
 * current step plus any pop-up. Back moves one step inside the feature, then leaves it.
 */
@Composable
private fun MeetPlannerScreenContent(onExit: () -> Unit, vm: MeetPlannerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var failure by remember { mutableStateOf<AppError?>(null) }
    var photos by remember { mutableStateOf<List<String>?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.useMyLocation() }
    // Turn-by-turn needs the phone's location too; once answered, try starting the trip again.
    val navPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.startNavigation() else Toast.makeText(context, R.string.meetplanner_toast_no_location, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        vm.effects.collect { fx ->
            when (fx) {
                is MeetEffect.Toast -> Toast.makeText(context, context.getString(fx.res, *fx.args.toTypedArray()), Toast.LENGTH_SHORT).show()
                is MeetEffect.Failure -> failure = fx.error
                is MeetEffect.Share -> context.startActivity(
                    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, fx.text), null),
                )
                is MeetEffect.Copy -> (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                    ?.setPrimaryClip(ClipData.newPlainText("itinerary", fx.text))
                is MeetEffect.OpenUrl -> openUrl(context, fx.url)
                is MeetEffect.ViewPhotos -> photos = fx.urls
                MeetEffect.NeedLocationPermission -> permission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
                MeetEffect.NeedNavPermission -> navPermission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
    failure?.let { err ->
        val text = err.asText()
        LaunchedEffect(err) { Toast.makeText(context, text, Toast.LENGTH_LONG).show(); failure = null }
    }

    BackHandler(enabled = state.selectMode) { vm.exitSelect() }
    BackHandler(enabled = !state.selectMode && state.stack.size > 1) { vm.back() }
    // Declared last so it wins: Back during turn-by-turn ends the trip and returns to the route preview.
    BackHandler(enabled = state.navActive) { vm.endNavigation() }

    TwinBackground {
        Box(Modifier.fillMaxSize().imePadding()) {
            when (state.screen) {
                MeetScreen.Home -> MeetHomeScreen(state, vm, onExit)
                MeetScreen.Plan -> PlanScreen(state, vm)
                MeetScreen.Search -> SearchScreen(state, vm)
                MeetScreen.Stops -> StopsScreen(state, vm)
                MeetScreen.Itinerary -> ItineraryScreen(state, vm)
                MeetScreen.Route -> RouteScreen(state, vm)
                MeetScreen.Saved -> SavedScreen(state, vm)
                MeetScreen.Detail -> DetailScreen(state, vm)
            }
        }
        state.placeSheet?.let { PlaceSheet(it, state.draft.isSelected(it), vm) { url -> openUrl(context, url) } }
        state.optionsId?.let { id -> state.meetups.firstOrNull { it.id == id }?.let { OptionsSheet(it, vm) } }
        state.completeId?.let { CompleteDialog(state, vm) }
        state.editId?.let { id -> state.meetups.firstOrNull { it.id == id }?.let { EditDialog(it, vm) } }
        state.deleteIds?.let { DeleteDialog(it.size, vm) }
        photos?.let { PhotoViewer(it) { photos = null } }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Public entry: the page body wrapped in the shared near-black glass look. */
@Composable
fun MeetPlannerScreen(onExit: () -> Unit, vm: MeetPlannerViewModel = hiltViewModel()) {
    MeetPlannerStyled { MeetPlannerScreenContent(onExit = onExit, vm = vm) }
}
