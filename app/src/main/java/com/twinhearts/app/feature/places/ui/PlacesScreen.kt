package com.twinhearts.app.feature.places.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.feature.places.ui.components.DeletePlaceDialog
import com.twinhearts.app.feature.places.ui.components.PhotoViewer
import com.twinhearts.app.feature.places.ui.components.PlaceFormDialog
import com.twinhearts.app.feature.places.ui.screens.PlaceDetailScreen
import com.twinhearts.app.feature.places.ui.screens.PlacesListScreen

/**
 * Why this file exists: the one entry point of Places. It owns the ViewModel, turns one-off effects
 * (toast, open maps, location permission) into Android calls, and shows the list, the detail page
 * and any pop-up. Back closes the top-most thing first, then leaves the feature.
 */
@Composable
fun PlacesScreen(onExit: () -> Unit, vm: PlacesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var failure by remember { mutableStateOf<AppError?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.useMyLocation() }

    LaunchedEffect(Unit) {
        vm.effects.collect { fx ->
            when (fx) {
                is PlacesEffect.Toast -> Toast.makeText(context, fx.res, Toast.LENGTH_SHORT).show()
                is PlacesEffect.Failure -> failure = fx.error
                is PlacesEffect.OpenUrl -> openUrl(context, fx.url)
                PlacesEffect.NeedLocationPermission -> permission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
    failure?.let { err ->
        val text = err.asText()
        LaunchedEffect(err) { Toast.makeText(context, text, Toast.LENGTH_LONG).show(); failure = null }
    }

    val somethingOpen = state.lightboxIndex != null || state.form != null || state.deleteId != null || state.detailId != null
    BackHandler(enabled = somethingOpen) { vm.back() }

    TwinBackground {
        Box(Modifier.fillMaxSize()) {
            if (state.detailId != null) PlaceDetailScreen(state, vm) else PlacesListScreen(state, vm, onExit)
        }
        state.form?.let { PlaceFormDialog(it, state.saving, vm) }
        state.deleteId?.let { DeletePlaceDialog(vm) }
        state.lightboxIndex?.let { PhotoViewer(state.detail?.photos.orEmpty(), it, vm) }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
