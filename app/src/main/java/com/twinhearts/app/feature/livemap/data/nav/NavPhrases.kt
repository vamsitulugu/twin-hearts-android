package com.twinhearts.app.feature.livemap.data.nav

import android.content.res.Resources
import com.twinhearts.app.R
import com.twinhearts.app.feature.livemap.domain.nav.ArriveSide
import com.twinhearts.app.feature.livemap.domain.nav.CueBand
import com.twinhearts.app.feature.livemap.domain.nav.ManeuverKind
import com.twinhearts.app.feature.livemap.domain.nav.VoiceCue
import java.util.Locale

/**
 * Turns manoeuvres into sentences, for the banner on screen and for the voice. All wording lives in
 * strings_livemap.xml (R-56). Needs only [Resources], so it works in the service and in Compose.
 */
class NavPhrases(private val res: Resources) {

    /** "Turn left onto Oak Avenue", "Take exit 2 at the roundabout", "Arrive at your destination". */
    fun instruction(kind: ManeuverKind, road: String, exit: Int?, side: ArriveSide?): String {
        val text = when {
            kind == ManeuverKind.Depart && road.isNotBlank() -> res.getString(R.string.livemap_nav_depart_on, road)
            road.isBlank() || kind == ManeuverKind.Arrive || kind == ManeuverKind.Depart -> action(kind, exit, side)
            else -> res.getString(R.string.livemap_nav_onto, action(kind, exit, side), road)
        }
        return text.replaceFirstChar { it.uppercase() }
    }

    private fun action(kind: ManeuverKind, exit: Int?, side: ArriveSide?): String = when (kind) {
        ManeuverKind.Depart -> res.getString(R.string.livemap_nav_depart)
        ManeuverKind.Arrive -> when (side) {
            ArriveSide.Left -> res.getString(R.string.livemap_nav_arrive_left)
            ArriveSide.Right -> res.getString(R.string.livemap_nav_arrive_right)
            else -> res.getString(R.string.livemap_nav_arrive)
        }
        ManeuverKind.Continue -> res.getString(R.string.livemap_nav_continue)
        ManeuverKind.TurnLeft -> res.getString(R.string.livemap_nav_turn_left)
        ManeuverKind.TurnRight -> res.getString(R.string.livemap_nav_turn_right)
        ManeuverKind.SlightLeft -> res.getString(R.string.livemap_nav_slight_left)
        ManeuverKind.SlightRight -> res.getString(R.string.livemap_nav_slight_right)
        ManeuverKind.SharpLeft -> res.getString(R.string.livemap_nav_sharp_left)
        ManeuverKind.SharpRight -> res.getString(R.string.livemap_nav_sharp_right)
        ManeuverKind.UTurn -> res.getString(R.string.livemap_nav_uturn)
        ManeuverKind.Merge -> res.getString(R.string.livemap_nav_merge)
        ManeuverKind.RampLeft -> res.getString(R.string.livemap_nav_ramp_left)
        ManeuverKind.RampRight -> res.getString(R.string.livemap_nav_ramp_right)
        ManeuverKind.ForkLeft -> res.getString(R.string.livemap_nav_fork_left)
        ManeuverKind.ForkRight -> res.getString(R.string.livemap_nav_fork_right)
        ManeuverKind.Roundabout ->
            if (exit != null && exit > 0) res.getString(R.string.livemap_nav_roundabout_exit, exit)
            else res.getString(R.string.livemap_nav_roundabout)
    }

    /** What the voice says for one cue, e.g. "In 300 metres, turn left onto Oak Avenue". */
    fun spoken(cue: VoiceCue): String {
        if (cue.isDeparture) {
            return if (cue.road.isBlank()) res.getString(R.string.livemap_nav_say_start)
            else res.getString(R.string.livemap_nav_say_start_on, cue.road)
        }
        val sentence = instruction(cue.kind, cue.road, cue.exit, cue.side)
        return when (cue.band) {
            CueBand.Now -> sentence
            else -> res.getString(R.string.livemap_nav_say_in, distance(cue.distanceM.toDouble()), sentence.replaceFirstChar { it.lowercase() })
        }
    }

    /** "300 metres", "1.2 kilometres". Spoken and shown the same way. */
    fun distance(meters: Double): String = if (meters < 1000) {
        res.getString(R.string.livemap_nav_metres, meters.toInt())
    } else {
        val km = String.format(Locale.getDefault(), "%.1f", meters / 1000.0).removeSuffix(".0").removeSuffix(",0")
        res.getString(R.string.livemap_nav_km, km)
    }
}
