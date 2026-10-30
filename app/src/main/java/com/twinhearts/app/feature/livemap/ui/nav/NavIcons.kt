package com.twinhearts.app.feature.livemap.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.ForkLeft
import androidx.compose.material.icons.rounded.ForkRight
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.RampLeft
import androidx.compose.material.icons.rounded.RampRight
import androidx.compose.material.icons.rounded.RoundaboutRight
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSharpLeft
import androidx.compose.material.icons.rounded.TurnSharpRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.ui.graphics.vector.ImageVector
import com.twinhearts.app.feature.livemap.domain.nav.ManeuverKind
import com.twinhearts.app.feature.livemap.domain.nav.TravelMode

/** The arrow shown for each manoeuvre, in the step list and on the navigation banner. */
fun ManeuverKind.icon(): ImageVector = when (this) {
    ManeuverKind.Depart -> Icons.Rounded.Navigation
    ManeuverKind.Arrive -> Icons.Rounded.Flag
    ManeuverKind.Continue -> Icons.Rounded.Straight
    ManeuverKind.TurnLeft -> Icons.Rounded.TurnLeft
    ManeuverKind.TurnRight -> Icons.Rounded.TurnRight
    ManeuverKind.SlightLeft -> Icons.Rounded.TurnSlightLeft
    ManeuverKind.SlightRight -> Icons.Rounded.TurnSlightRight
    ManeuverKind.SharpLeft -> Icons.Rounded.TurnSharpLeft
    ManeuverKind.SharpRight -> Icons.Rounded.TurnSharpRight
    ManeuverKind.UTurn -> Icons.Rounded.UTurnLeft
    ManeuverKind.Merge -> Icons.Rounded.Merge
    ManeuverKind.RampLeft -> Icons.Rounded.RampLeft
    ManeuverKind.RampRight -> Icons.Rounded.RampRight
    ManeuverKind.ForkLeft -> Icons.Rounded.ForkLeft
    ManeuverKind.ForkRight -> Icons.Rounded.ForkRight
    ManeuverKind.Roundabout -> Icons.Rounded.RoundaboutRight
}

fun TravelMode.icon(): ImageVector = when (this) {
    TravelMode.Drive -> Icons.Rounded.DirectionsCar
    TravelMode.Walk -> Icons.Rounded.DirectionsWalk
    TravelMode.Bike -> Icons.Rounded.DirectionsBike
}
